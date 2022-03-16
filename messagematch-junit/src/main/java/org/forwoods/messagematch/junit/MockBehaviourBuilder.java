package org.forwoods.messagematch.junit;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.forwoods.messagematch.generate.JsonGenerator;
import org.forwoods.messagematch.spec.CallExample;
import org.forwoods.messagematch.spec.MethodCallChannel;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.stubbing.OngoingStubbing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;

import static org.mockito.ArgumentMatchers.*;

public class MockBehaviourBuilder extends BehaviourBuilder {

    public static ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void addBehavior(Collection<CallExample> calls) {
        calls.stream().filter(c->c.getChannel() instanceof MethodCallChannel).forEach(c->{
                MethodCallChannel channel = (MethodCallChannel) c.getChannel();
                Class<?> mockClass = getClass(channel.getClassName());
            addBehaviour(c, channel, mockClass);
        });
    }

    private <T> void addBehaviour(CallExample c, MethodCallChannel channel, Class<T> mockClass) {
        Object o =mocks.get(mockClass);
        if (o==null) {
            throw new RuntimeException("No mock of class "+ channel.getClassName()+"found");
        }
        addBehavior(mockClass, mockClass.cast(o), channel, c.getRequestMessage(), c.getResponseMessage());
    }

    private <T> void addBehavior(Class<T> mockClass, T mock, MethodCallChannel channel, JsonNode argumentValues, JsonNode response) {
        Class<?>[] paramsTypes = Arrays.stream(channel.getMethodArgs()).map(this::getClass).toArray(Class[]::new);
        try {
            Method m = mockClass.getMethod(channel.getMethodName(), paramsTypes);
            ArrayNode args = (ArrayNode) argumentValues;
            Object[] paramMatchers = new Object[paramsTypes.length];
            for (int i=0;i<paramsTypes.length;i++) {
                paramMatchers[i] = buildMatcher(paramsTypes[i], args.get(i));
            }
            //Object[] p= StreamSupport.stream(args.spliterator(),false).map(n->intThat(new MessageArgumentMatcher(n))).toArray();
            Object o = m.invoke(mock, paramMatchers);
            OngoingStubbing<Object> when = Mockito.when(o);
            when.thenAnswer(invocation -> {
                JsonNode generate = new JsonGenerator(response).generate();
                Type t = m.getGenericReturnType();
                JavaType jt = TypeFactory.defaultInstance().constructType(t);
                return objectMapper.treeToValue(generate, jt);
            });


        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Method "+channel.getMethodName()+"("+Arrays.toString(channel.getMethodArgs())
                    +") not found on "+channel.getClassName());
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Error magic mocking Method "+channel.getMethodName()+"("+ Arrays.toString(channel.getMethodArgs())
                    +") not found on "+channel.getClassName());
        }
    }

    @SuppressWarnings("unchecked")
    private Object buildMatcher(Class<?> paramsType, JsonNode jsonNode) {
        MessageArgumentMatcher<?> matcher = new MessageArgumentMatcher<>(jsonNode);
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


}
