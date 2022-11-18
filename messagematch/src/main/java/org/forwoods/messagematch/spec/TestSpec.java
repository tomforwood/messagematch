package org.forwoods.messagematch.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class TestSpec {

    public static final String TEST_SPEC = ".testSpec";

    public static final ObjectMapper specParser = new ObjectMapper(new YAMLFactory());

    final CallExample<?> callUnderTest;
    private final List<TriggeredCall<?>> sideEffects;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    @SuppressWarnings("rawtypes")//These have to be raw otherwise jackson does something stupid and fails to use the type field in channel
    public TestSpec(@JsonProperty("callUnderTest")CallExample callUnderTest,
                    @JsonProperty("sideEffects")List<TriggeredCall> sideEffects) {
        this.callUnderTest = callUnderTest;
        this.sideEffects = sideEffects==null?List.of():sideEffects.stream().map(t->(TriggeredCall<?>)t).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public <C extends Channel> CallExample<C> getCallUnderTest() {
        return (CallExample<C>) callUnderTest;
    }

    public List<TriggeredCall<?>> getSideEffects() {
        return sideEffects;
    }

    public TestSpec resolve(URL base) {
        callUnderTest.resolve(base);
        sideEffects.forEach(s->s.getCall().resolve(base));
        return this;
    }
}
