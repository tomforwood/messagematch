package org.forwoods.messagematch.springjdbc;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.forwoods.messagematch.generate.JsonGenerator;
import org.forwoods.messagematch.junit.BehaviourBuilder;
import org.forwoods.messagematch.match.JsonMatcher;
import org.forwoods.messagematch.spec.*;
import org.mockito.Answers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mockito.Mockito.*;

public class SpringJdbcTemplateBehaviourBuilder extends BehaviourBuilder {


    private JdbcTemplate template;

    //This behaviour insists on being allowed to construct its own mock so it can set the default behaviour for any method
    public JdbcTemplate getTemplate() {
        return template==null?
                this.template= mock(JdbcTemplate.class, templateObjectAnswer):
                this.template;
    }

    TemplateObjectAnswer templateObjectAnswer = new TemplateObjectAnswer();

    @Override
    public void addBehavior(Collection<TriggeredCall> calls) {
        calls.stream().filter(this::isTemplateCall).forEach(this::addBehavior);
    }

    private void addBehavior(TriggeredCall call) {
        if (getGenericChannel(call).isEmpty()) return;
        GenericChannel channel = getGenericChannel(call).get();
        String method = channel.getProperties().get("returns");
        switch (method) {
            case "object" :
                try {
                    String objectType = channel.getProperties().get("objectType");
                    Class<?> clazz = Class.forName(objectType);
                    JavaType type = TestSpec.specParser.getTypeFactory().constructType(clazz);
                    templateObjectAnswer.calls.put(call.getCall(), type);
                }
                catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "list" :
                try {
                    String objectType = channel.getProperties().get("objectType");
                    Class<?> clazz = Class.forName(objectType);
                    JavaType type = TestSpec.specParser.getTypeFactory().constructCollectionType(ArrayList.class, clazz);
                    templateObjectAnswer.calls.put(call.getCall(), type);
                }
                catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "int" :
                JavaType type = TestSpec.specParser.getTypeFactory().constructType(Integer.TYPE);
                templateObjectAnswer.calls.put(call.getCall(), type);
                break;

        }
    }

    private boolean isTemplateCall(TriggeredCall triggeredCall) {
        Optional<GenericChannel> genOpt = getGenericChannel(triggeredCall);
        return genOpt.isPresent();
    }

    private Optional<GenericChannel> getGenericChannel(TriggeredCall triggeredCall) {
        Optional<TriggeredCall> callOpt = Optional.of(triggeredCall);
        return callOpt.map(c-> c.getCall().getChannel()).filter(c->c instanceof GenericChannel)
                .map(GenericChannel.class::cast)
                .filter(gc->gc.getTypeName().equals("jdbcTemplate"));
    }

    @Override
    protected Class<? extends Channel> getChannelType() {
        return GenericChannel.class;
    }

    private class TemplateObjectAnswer implements Answer<Object> {

        Map<CallExample, JavaType> calls = new HashMap<>();

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            if (invocation.getArguments().length>=2) {
                String query = invocation.getArgument(0);
                Object[] objects = getParams(invocation);
                for (Map.Entry<CallExample, JavaType> entry : calls.entrySet()) {
                    //TODO ensure that these casts and gets will work at addBehaviour time/work out how to do this for mare calls than the ones tested
                    ObjectNode requestNode = (ObjectNode) entry.getKey().getRequestMessage();
                    String queryToMatch = requestNode.get("query").asText();
                    ArrayNode paramsToMatch = (ArrayNode) requestNode.get("params");
                    boolean matches;
                    if (queryToMatch.startsWith("^")) {
                        //treat this as a regex - sadly you can't get the names of named groups out of the pattern without reflection
                        Pattern p = Pattern.compile(queryToMatch);
                        Matcher matcher = p.matcher(query);
                        matches = matcher.matches();
                    } else {
                        matches = queryToMatch.equals(query);
                    }
                    //JsonMatcher queryMatcher = new JsonMatcher(queryToMatch, query);
                    if (matches) {
                        JsonNode paramsNode = TestSpec.specParser.valueToTree(objects);
                        JsonMatcher paraMatcher = new JsonMatcher(paramsToMatch, paramsNode);
                        if (paraMatcher.matches()) {
                            Map<String, Object> bindings = new HashMap<>(paraMatcher.getBindings());
                            callsMatched.getOrDefault(entry.getKey(), new ArrayList<>()).add(new Invocation(bindings));
                            JsonGenerator generator = new JsonGenerator(entry.getKey().getResponseMessage(), bindings);
                            JsonNode resNode = generator.generate();
                            return TestSpec.specParser.treeToValue(resNode, entry.getValue());
                        }
                    }
                }
            }
            return Answers.RETURNS_SMART_NULLS.answer(invocation);
        }

        private final Class<?> objectArrayType =Array.newInstance(Object.class, 0).getClass();

        //This probably doesn't quite work for your use case - it works for mine right now
        //hopefully it can be expanded
        private Object[] getParams(InvocationOnMock invocation) {
            Method m = invocation.getMethod();
            Class<?>[] params = m.getParameterTypes();
            if (params.length < 2) return new Object[]{};
            if (params[1].equals(objectArrayType)) return invocation.getArgument(1);
            if (params.length < 3) return new Object[]{};

            if (m.isVarArgs()) {
                Object[] args = invocation.getArguments();
                //This has split varargs into separate arguments - I need to put them back together
                return Arrays.copyOfRange(args, 2, args.length);
            }
            return invocation.getArgument(2);
        }
    }
}
