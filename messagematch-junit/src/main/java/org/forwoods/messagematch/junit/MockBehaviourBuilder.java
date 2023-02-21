package org.forwoods.messagematch.junit;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.forwoods.messagematch.generate.JsonGenerator;
import org.forwoods.messagematch.spec.CallExample;
import org.forwoods.messagematch.spec.MethodCallChannel;
import org.forwoods.messagematch.spec.TestSpec;
import org.forwoods.messagematch.spec.TriggeredCall;
import org.mockito.ArgumentMatcher;
import org.mockito.stubbing.Stubber;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;

public class MockBehaviourBuilder extends BehaviourBuilder<MethodCallChannel> {

    @Override
    public void addFilteredBehavior(Stream<TriggeredCall<MethodCallChannel>> calls) {
        calls.map(TriggeredCall::getCall).forEach(c->{
                MethodCallChannel channel = c.getChannel();
                Class<?> mockClass = getClass(channel.getClassName());
            addBehaviour(c, mockClass);
        });
    }

    private <T> void addBehaviour(CallExample<MethodCallChannel> c, Class<T> mockClass) {
        Object o =mocks.get(mockClass);
        if (o==null) {
            throw new RuntimeException("No mock of class "+ c.getChannel().getClassName()+"found");
        }
        addBehavior(mockClass, mockClass.cast(o), c, c.getChannel(), c.getRequestMessage(), c.getResponseMessage());
    }

    private <T> void addBehavior(Class<T> mockClass, T mock, CallExample<MethodCallChannel> call,  MethodCallChannel channel, JsonNode argumentValues, JsonNode response) {
        Class<?>[] paramsTypes = Arrays.stream(channel.getMethodArgs()).map(this::getClass).toArray(Class[]::new);
        try {
            Method m = mockClass.getMethod(channel.getMethodName(), paramsTypes);
            MessageArgumentMatcher<?>[] matchers = new MessageArgumentMatcher[paramsTypes.length];
            Stubber stubber = doAnswer(invocation -> {
                JsonNode generate = new JsonGenerator(response).generate();
                JavaType jt;
                if(channel.getReturnType()==null){
                    Type t = m.getGenericReturnType();
                    jt= TypeFactory.defaultInstance().constructType(t);}
                else {
                    jt = TypeFactory.defaultInstance().constructType(Class.forName(channel.getReturnType()));
                }
                Map<String, Object> bindings = Arrays.stream(matchers).map(MessageArgumentMatcher::getBindings)
                        .reduce(new HashMap<>(), (m1, m2) -> {
                            m1.putAll(m2);
                            return m1;
                        });
                callsMatched.computeIfAbsent(call, c -> new ArrayList<>()).add(new Invocation(bindings));
                return TestSpec.specParser.treeToValue(generate, jt);
            });
            T when1 = stubber.when(mock);
            if (!(argumentValues instanceof ArrayNode)){
                throw new RuntimeException("Arguments to mocks expected to be an array");
            }
            invokeMethod(when1, (ArrayNode) argumentValues, paramsTypes, m, matchers);

        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Method "+channel.getMethodName()+"("+Arrays.toString(channel.getMethodArgs())
                    +") not found on "+channel.getClassName());
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Error magic mocking Method "+channel.getMethodName()+"("+ Arrays.toString(channel.getMethodArgs())
                    +") not found on "+channel.getClassName());
        }
    }

    private <T> void invokeMethod(T mock, ArrayNode argumentValues, Class<?>[] paramsTypes, Method m,
                                  MessageArgumentMatcher<?>[] matchers) throws IllegalAccessException, InvocationTargetException {
        Object[] paramMatchers = new Object[paramsTypes.length];
        for (int i = 0; i< paramsTypes.length; i++) {
            matchers[i]= new MessageArgumentMatcher<>(argumentValues.get(i));
            paramMatchers[i] = buildMatcher(paramsTypes[i], matchers[i]);
        }
        m.invoke(mock, paramMatchers);
    }

    @Override
    protected Class<MethodCallChannel> getChannelType() {
        return MethodCallChannel.class;
    }

    @SuppressWarnings("unchecked")
    private Object buildMatcher(Class<?> paramsType, MessageArgumentMatcher<?> matcher) {
        if (int.class.equals(paramsType)) {
            return intThat((ArgumentMatcher<Integer>) matcher);
        }
        else if (byte.class.equals(paramsType)) {
            return byteThat((ArgumentMatcher<Byte>) matcher);
        }
        else if (short.class.equals(paramsType)) {
            return shortThat((ArgumentMatcher<Short>) matcher);
        }
        else if (long.class.equals(paramsType)) {
            return longThat((ArgumentMatcher<Long>) matcher);
        }
        else if (float.class.equals(paramsType)) {
            return floatThat((ArgumentMatcher<Float>) matcher);
        }
        else if (double.class.equals(paramsType)) {
            return doubleThat((ArgumentMatcher<Double>) matcher);
        }
        else if (boolean.class.equals(paramsType)) {
            return booleanThat((ArgumentMatcher<Boolean>) matcher);
        }
        else {
            return argThat(matcher);
        }
    }

    private Class<?> getClass(String c) {
        switch (c) {
            case "int": return int.class;
            case "byte" : return byte.class;
            case "short": return short.class;
            case "long": return long.class;
            case "float": return float.class;
            case "double": return double.class;
            case "boolean": return boolean.class;
            default:
                try {
                    return Class.forName(c);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("mocked class "+c+ " not found",e);
                }
        }

    }

    @Override
    public void verifyBehaviour(Collection<TriggeredCall<?>> calls) throws BehaviourVerificationException {
        super.verifyBehaviour(calls);
    }
}
