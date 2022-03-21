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
        CallExample example = new CallExample();
        example.setChannel(new MethodCallChannel("java.util.List","get",new String[]{int.class.getName()}));
        example.setRequestMessage(TestSpec.specParser.readTree("[\"$Int\"]"));
        example.setResponseMessage(TestSpec.specParser.readTree("103"));
        TriggeredCall tc = new TriggeredCall(new TriggeredCall.Times(1,1), example, null, null, null, null,null,null);
        builder.addBehavior(List.of(tc));

        Integer result = list.get(5);
        assertEquals(103, result);
    }
}