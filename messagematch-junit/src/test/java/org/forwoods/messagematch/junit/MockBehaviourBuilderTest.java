package org.forwoods.messagematch.junit;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.forwoods.messagematch.spec.CallExample;
import org.forwoods.messagematch.spec.MethodCallChannel;
import org.forwoods.messagematch.spec.TestSpec;
import org.forwoods.messagematch.spec.TriggeredCall;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class MockBehaviourBuilderTest {
    @Test
    void testBehaviourAdd() throws JsonProcessingException {
        @SuppressWarnings("unchecked")
        List<Integer> list = mock(List.class);
        MockBehaviourBuilder builder = new MockBehaviourBuilder();
        builder.addMocks(Map.of(List.class, list));
        CallExample<MethodCallChannel> example = new CallExample<>();
        example.setChannel(new MethodCallChannel("java.util.List","get",new String[]{int.class.getName()}));
        example.setRequestMessage(TestSpec.specParser.readTree("[\"$Int=readIndex\"]"));
        example.setResponseMessage(TestSpec.specParser.readTree("103"));
        TriggeredCall<MethodCallChannel> tc = new TriggeredCall<>(new TriggeredCall.Times(1,1), example, null, null, null, null,null,null);
        List<TriggeredCall<?>> calls = List.of(tc);
        builder.addBehavior(calls);

        Integer result = list.get(5);

        assertEquals(103, result);
        assertEquals("5", builder.callsMatched.get(example).get(0).bindings.get("readIndex"));
        builder.verifyBehaviour(calls);
    }

    @Test
    void testVerifyuFAils() throws JsonProcessingException {
        @SuppressWarnings("unchecked")
        List<Integer> list = mock(List.class);
        MockBehaviourBuilder builder = new MockBehaviourBuilder();
        builder.addMocks(Map.of(List.class, list));
        CallExample<MethodCallChannel> example = new CallExample<>();
        example.setChannel(new MethodCallChannel("java.util.List","get",new String[]{int.class.getName()}));
        example.setRequestMessage(TestSpec.specParser.readTree("[\"$Int=myVal\"]"));
        example.setResponseMessage(TestSpec.specParser.readTree("103"));
        TriggeredCall<MethodCallChannel> tc = new TriggeredCall<>(new TriggeredCall.Times(1,1), example, null, null, null, null,null,null);
        List<TriggeredCall<?>> calls = List.of(tc);
        builder.addBehavior(calls);
        //we don't actually make the expected call so the verify should fail
        assertThrows(BehaviourVerificationException.class, ()->builder.verifyBehaviour(calls));
    }

    @Test
    void testBehaviourNoArgs() throws JsonProcessingException {
        @SuppressWarnings("unchecked")
        List<Integer> list = mock(List.class);
        MockBehaviourBuilder builder = new MockBehaviourBuilder();
        builder.addMocks(Map.of(List.class, list));
        CallExample<MethodCallChannel> example = new CallExample<>();
        example.setChannel(new MethodCallChannel("java.util.List","size",new String[]{}));
        example.setRequestMessage(TestSpec.specParser.readTree("[]"));
        example.setResponseMessage(TestSpec.specParser.readTree("103"));
        TriggeredCall<MethodCallChannel> tc = new TriggeredCall<>(new TriggeredCall.Times(1,1), example, null, null, null, null,null,null);
        List<TriggeredCall<?>> calls = List.of(tc);
        builder.addBehavior(calls);

        Integer result = list.size();
        assertEquals(103, result);
        builder.verifyBehaviour(calls);
    }
}