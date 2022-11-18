package org.forwoods.messagematch.junit;

import com.fasterxml.jackson.databind.JsonNode;
import org.forwoods.messagematch.match.JsonMatcher;
import org.forwoods.messagematch.spec.TestSpec;
import org.mockito.ArgumentMatcher;

import java.util.Map;

public class MessageArgumentMatcher<T> implements ArgumentMatcher<T> {

    private final JsonNode matcher;
    private Map<String, Object> bindings;

    public MessageArgumentMatcher(JsonNode matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T argument) {
        JsonMatcher matcher = new JsonMatcher(this.matcher, TestSpec.specParser.valueToTree(argument));
        boolean matches = matcher.matches();
        this.bindings = matcher.getBindings();
        return matches;
    }

    public String toString() {
        return matcher.toString();
    }

    public Map<String, Object> getBindings() {
        return bindings;
    }
}
