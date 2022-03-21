package org.forwoods.messagematch.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.net.URL;
import java.util.List;

public class TestSpec {

    public static final String TEST_SPEC = ".testSpec";

    public static final ObjectMapper specParser = new ObjectMapper(new YAMLFactory());

    CallExample callUnderTest;
    private final List<TriggeredCall> sideEffects;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public TestSpec(@JsonProperty("callUnderTest")CallExample callUnderTest,
                    @JsonProperty("sideEffects")List<TriggeredCall> sideEffects) {
        this.callUnderTest = callUnderTest;
        this.sideEffects = sideEffects==null?List.of():sideEffects;
    }

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public CallExample getCallUnderTest() {
        return callUnderTest;
    }

    public List<TriggeredCall> getSideEffects() {
        return sideEffects;
    }

    public TestSpec resolve(URL base) {
        callUnderTest.resolve(base);
        sideEffects.forEach(s->s.getCall().resolve(base));
        return this;
    }
}
