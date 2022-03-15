package org.forwoods.messagematch.junit;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.forwoods.messagematch.spec.CallExample;
import org.forwoods.messagematch.spec.MethodCallChannel;
import org.forwoods.messagematch.spec.TestSpec;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.OngoingStubbing;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.intThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MockBehaviourBuilderTest {
    @Test
    void testBehaviourAdd() throws JsonProcessingException {
        List<Integer> list = mock(List.class);
        MockBehaviourBuilder builder = new MockBehaviourBuilder();
        builder.addMocks(Map.of(List.class, list));
        CallExample example = new CallExample();
        example.setChannel(new MethodCallChannel("java.util.List","get",new String[]{int.class.getName()}));
        example.setRequestMessage(TestSpec.specParser.readTree("[\"$Int\"]"));
        example.setResponseMessage(TestSpec.specParser.readTree("103"));
        builder.addBehavior(List.of(example));

        Integer result = list.get(5);
        assertEquals(103, result);
    }
}