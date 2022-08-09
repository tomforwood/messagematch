package org.forwoods.messagematch.junit;

import com.fasterxml.jackson.databind.JsonNode;
import org.forwoods.messagematch.match.JsonMatcher;
import org.mockito.ArgumentMatcher;

public class MessageArgumentMatcher<T> implements ArgumentMatcher<T> {

    private final JsonNode matcher;

    public MessageArgumentMatcher(JsonNode matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(T argument) {
        return new JsonMatcher(matcher, MockBehaviourBuilder.objectMapper.valueToTree(argument)).matches();
    }

    public String toString() {
        return matcher.toString();
    }
}
