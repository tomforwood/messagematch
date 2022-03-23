package org.forwoods.messagematch.spec;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class EventSpecTest {
    @Test
    public void specResolveTest() throws IOException {
        URL resource = getClass().getClassLoader().getResource("specs/test.testSpec");
        TestSpec parent = TestSpec.specParser.readValue(resource, TestSpec.class);
        parent = parent.resolve(resource);
        assertNotNull(parent.getSideEffects().get(0).getCall().getChannel());
        assertEquals("post:/myOtherService",parent.getSideEffects().get(0).getCall().getChannel().toString());
    }
}