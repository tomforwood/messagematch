package org.forwoods.messagematch.apiscenario;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.forwoods.messagematch.apiscenario.spec.APITestScenario;
import org.forwoods.messagematch.apiscenario.spec.State;
import org.forwoods.messagematch.generate.JsonGenerator;
import org.forwoods.messagematch.match.JsonMatcher;
import org.forwoods.messagematch.match.PathMatcher;
import org.forwoods.messagematch.spec.CallExample;
import org.forwoods.messagematch.spec.URIChannel;
import org.apache.commons.lang3.StringUtils;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ScenarioMatcher {
    private final APITestScenario scenario;
    public int state;
    private final Map<String, Object> bindings = new ConcurrentHashMap<>();

    public ScenarioMatcher(final APITestScenario scenario) {
        this.scenario = scenario;
    }

    public HttpResponse findMatchHttp(String path, String body, String method) {
        final State currentState = scenario.getExpectedStates().get(state);
        boolean matched;
        for (int i = 0; i < currentState.getExpectedCalls().size(); i++) {
            try {
                final CallExample<URIChannel> expectedCall = currentState.getExpectedCalls().get(i);


                //the attempt to match the body will update binding - if we fail we don't want the bindings to stick around
                Map<String, Object> updatedBinding = new HashMap<>(bindings);
                matched = matches(expectedCall, path, body, method, updatedBinding);
                if (matched) {
                    System.out.println("matched body");
                    return returnSucessfulMatch(expectedCall, updatedBinding);
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

        }
        if (state < scenario.getExpectedStates().size() - 1) {
            int newStateNum = state + 1;
            final State newState = scenario.getExpectedStates().get(newStateNum);
            try {
                final CallExample<URIChannel> stateTrigger = newState.getStateTrigger();
                Map<String, Object> updatedBinding = new HashMap<>(bindings);
                matched = matches(stateTrigger, path, body, method, updatedBinding);
                if (matched) {
                    return returnSucessfulMatch(stateTrigger, updatedBinding);
                }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

        }
        return null;
    }

    private boolean matches(CallExample<URIChannel> expectedCall, String path, String body, String method, final Map<String, Object> updatedBinding) throws JsonProcessingException {
        if (!expectedCall.getChannel().getMethod().equals(method)){
            return false;
        }
        final PathMatcher pathMatcher = new PathMatcher(expectedCall.getChannel().getPath(), path, updatedBinding);
        if (!pathMatcher.matches()) {
//                    System.out.println("path did not matched " + path);
            return false;
        }
        //the path matcher makes its own copy so we put these in the attempt bindings
        updatedBinding.putAll(pathMatcher.getBindings());
        //TODO match headers on the request
        if (expectedCall.getRequestMessage().isEmpty() && StringUtils.isBlank(body)) {
            return true;
        } else {
            final JsonNode requestMessage = expectedCall.getRequestMessage();
            JsonMatcher matcher = new JsonMatcher(requestMessage, body);
            if (matcher.matches()) {
                updatedBinding.putAll(matcher.getBindings());
                return true;
            }
        }
        return false;
    }

    private HttpResponse returnSucessfulMatch(CallExample<URIChannel> expectedCall, Map<String, Object> bindings) {
        HttpResponse response;
        final int statusCode = expectedCall.getChannel().getStatusCode() == 0 ? 200 : expectedCall.getChannel().getStatusCode();
        if (expectedCall.getResponseMessage() != null) {
            JsonGenerator generator = new JsonGenerator(expectedCall.getResponseMessage(), bindings);
            final JsonNode generatedJson = generator.generate();
            response = new HttpResponse(statusCode, generatedJson);
        } else {
            response = new HttpResponse(statusCode, null);
        }
        this.bindings.putAll(bindings);
        return response;
    }

    public int getState() {
        return state;
    }

    public record HttpResponse(int statusCode, JsonNode responseBody) {
    }
}
