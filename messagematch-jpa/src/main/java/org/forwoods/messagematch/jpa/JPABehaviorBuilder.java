package org.forwoods.messagematch.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.forwoods.messagematch.generate.JsonGenerator;
import org.forwoods.messagematch.junit.BehaviourBuilder;
import org.forwoods.messagematch.junit.BehaviourVerificationException;
import org.forwoods.messagematch.match.JsonMatcher;
import org.forwoods.messagematch.match.MatchError;
import org.forwoods.messagematch.spec.CallExample;
import org.forwoods.messagematch.spec.GenericChannel;
import org.forwoods.messagematch.spec.TestSpec;
import org.forwoods.messagematch.spec.TriggeredCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.repository.CrudRepository;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JPABehaviorBuilder extends BehaviourBuilder<GenericChannel> {
    private final static Logger logger = LoggerFactory.getLogger(JPABehaviorBuilder.class);

    private final List<?> persisters;

    private final Map<String, Object> bindings = new HashMap<>();

    public JPABehaviorBuilder(List<?> persisters) {
        this.persisters = persisters;
    }

    @Override
    protected void addFilteredBehavior(Stream<TriggeredCall<GenericChannel>> calls) {
        calls.forEach(this::addBehavior);
    }

    private void addBehavior(TriggeredCall<GenericChannel> call) {
        Action action = getAction(call);
        if (action==Action.SETUP) {
            Class<?> persisterClass = getPersisterClass(call);
            doInsert(persisterClass, call.getCall());
        }

    }

    private static Action getAction(TriggeredCall<GenericChannel> call) {
        return Action.valueOf(call.getCall().getChannel().getProperties().get("action"));
    }

    private <T> void doInsert(Class<T> persisterClass, CallExample<GenericChannel> call) {
        T t = findPersister(persisterClass);
        if (t instanceof CrudRepository) {
            doCrudInsert(persisterClass, (CrudRepository<?,?>) t, call);
        }
        else {
            throw new RuntimeException("Expected "+persisterClass+" to be a CRUDRepository");
        }
    }

    private <Q, T extends CrudRepository<Q,?>> void doCrudInsert(Class<?> persisterClass, T repo, CallExample<GenericChannel> call) {
        Type actualTypeArgument = ((ParameterizedType) getGenericInterface(persisterClass)).getActualTypeArguments()[0];

        for (int i=0;i<call.getRequestMessage().size();i++) {
            JsonNode toInsert = call.getRequestMessage().get(i);
            try {
                JsonGenerator generator = new JsonGenerator(toInsert, bindings);
                toInsert = generator.generate();
                JsonNode binding = call.getResponseMessage().get(i);
                @SuppressWarnings("unchecked")
                Q toSave = TestSpec.specParser.treeToValue(toInsert, (Class<Q>)actualTypeArgument);
                Q res = repo.save(toSave);
                if (binding!=null) {
                    bindings.put(binding.textValue(), TestSpec.specParser.valueToTree(res));
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error persisting object "+toInsert,e);
            }
        }
    }

    private static Type getGenericInterface(Class<?> persisterClass) {
        return Arrays.stream(persisterClass.getGenericInterfaces())
                .filter(ParameterizedType.class::isInstance)
                .map(ParameterizedType.class::cast)
                .filter(c->c.getRawType().equals(CrudRepository.class)).findFirst().orElseThrow();
    }

    private <T> T findPersister(Class<T> persisterClass) {
        return persisters.stream().filter(p->persisterClass.isAssignableFrom(p.getClass())).map(persisterClass::cast)
                .findFirst()
                .orElseThrow(()->new RuntimeException("Cannot find persister of class "+persisterClass));
    }

    @Override
    protected Class<GenericChannel> getChannelType() {
        return GenericChannel.class;
    }

    @Override
    protected Stream<TriggeredCall<GenericChannel>> filteredCalls(Collection<TriggeredCall<?>> calls) {
        Stream<TriggeredCall<GenericChannel>> rightChannel =  super.filteredCalls(calls);
        return rightChannel.filter(c->c.getCall().getChannel().getTypeName().equals("jpa"));
    }

    @Override
    public void verifyBehaviour(Collection<TriggeredCall<?>> calls) throws BehaviourVerificationException {
        Map<? extends Class<?>, List<TriggeredCall<GenericChannel>>> persisterCalls = filteredCalls(calls).filter(c -> getAction(c) == Action.CHECK)
                .collect(Collectors.groupingBy(this::getPersisterClass));
        List<String> errors = persisterCalls.entrySet().stream().flatMap(e -> checkCalls(e.getKey(), e.getValue())).toList();
        if (!errors.isEmpty()) {
            throw new BehaviourVerificationException(String.join("\n",errors));
        }
    }

    private Class<?> getPersisterClass(TriggeredCall<GenericChannel> call) {
        try {
            String persisterName = call.getCall().getChannel().getProperties().get("persister");
            return Class.forName(persisterName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> Stream<String> checkCalls(Class<T> persisterClass, List<TriggeredCall<GenericChannel>> expectedCalls) {
        T t = findPersister(persisterClass);
        if (t instanceof CrudRepository) {
            List<JsonNode>  expectedRows = expectedCalls.stream()
                    .map(c -> c.getCall().getRequestMessage())
                    .flatMap(this::toStream)
                    .toList();
            List<JsonNode> inserted = readRepo((CrudRepository<?, ?>) t).stream().<JsonNode>map(TestSpec.specParser::valueToTree).toList();
            if (expectedRows.size()==1 && inserted.size()==1) {
                //if there is one of each then we can do stricter matching
                JsonNode actual = inserted.get(0);
                JsonMatcher matcher = new JsonMatcher(expectedRows.get(0),actual);
                if (!matcher.matches()) {
                    return matcher.getErrors().stream().map(MatchError::toString);
                }
            }
            else {
                List<JsonNode> unmatched  =new ArrayList<>();
                for (JsonNode node:expectedRows) {
                    boolean matched = false;
                    for (JsonNode actual:inserted) {
                        JsonMatcher matcher = new JsonMatcher(expectedRows.get(0),actual);
                        if (matcher.matches()) {
                            matched=true;
                            break;
                        }
                        else {
                            logger.trace(matcher.getErrors().toString());
                        }
                    }
                    if (!matched) {
                        unmatched.add(node);
                    }
                }
                if (!unmatched.isEmpty()) {
                    return Stream.of("For persister "+persisterClass + " Expected values matching "+ unmatched + "\nbut found values " + inserted);
                }
            }
        }
        else {
            throw new RuntimeException("Expected "+persisterClass+" to be a CRUDRepository");
        }
        return Stream.of();
    }

    private Stream<JsonNode> toStream(JsonNode r) {
        return StreamSupport.stream(r.spliterator(), false);
    }

    private <Q, T extends CrudRepository<Q,?>> List<Q> readRepo(T repo) {
        Iterable<Q> data= repo.findAll();
        List<Q> insertedList = new ArrayList<>();
        data.forEach(insertedList::add);
        return insertedList;
    }

    enum Action {
        SETUP, CHECK
    }
}
