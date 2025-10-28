package org.forwoods.messagematch.spec;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventSpecTest {
    @Test
    public void specResolveTest() throws IOException {
        URL resource = getClass().getClassLoader().getResource("specs/test.testSpec");
        TestSpec parent = TestSpec.specParser.readValue(resource, TestSpec.class);
        parent = parent.resolve(resource);
        assertNotNull(parent.getSideEffects().get(0).getCall().getChannel());
        assertEquals("URIChannel{path='/myOtherService', method='post', statusCode=0, statusLine='null'}",parent.getSideEffects().get(0).getCall().getChannel().toString());
    }

    @Test
    public void specWriteTest() throws IOException {
        CallExample<URIChannel> call = new CallExample<>();
        call.setChannel(new URIChannel("blah", "get", 200, "ok"));

        TestSpec spec = new TestSpec(call, List.of());
        String s= TestSpec.specParser.writeValueAsString(spec);
        System.out.println(s);
        ObjectMapper mapper = new ObjectMapper();
        s= mapper.writeValueAsString(spec);
        System.out.println(s);
        //{"reference":null,"relative":null,"name":null,"channel":{"@type":"uri","path":"blah","method":"get"},"requestMessage":null,"responseMessage":null,"schema":null}
        s = "{\"callUnderTest\":{\"reference\":null,\"relative\":null,\"name\":null,\"channel\":{\"@type\":\"uri\",\"path\":\"blah\",\"method\":\"get\"},\"requestMessage\":null,\"responseMessage\":null,\"schema\":null},\"sideEffects\":[]}";
        mapper.readValue(s, TestSpec.class);

    }
}