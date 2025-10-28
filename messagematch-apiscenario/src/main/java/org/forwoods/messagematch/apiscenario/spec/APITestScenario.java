package org.forwoods.messagematch.apiscenario.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.util.List;

/**
 * When being used by a mock server the server will start in the Initial State
 * Whenever a call is received by the server it will attempt to match it to a callexample in the current state
 * If it matches that call example will be used to generate a reply
 * If no call in the current state matches the incoming call then it is compared to the stateTrigger call in the next call
 * if the trigger matches the appropriate response is returned and the server switches into the new state
 *
 * if no match has been found the server will error
 */
public class APITestScenario
{
    public static final String API_SPEC = ".apiScenario";
    public static final ObjectMapper specParser = new ObjectMapper(new YAMLFactory());

    private final String scenarioName;
    private final List<State> expectedStates;

    @JsonCreator
    public APITestScenario(@JsonProperty("scenarioName") final String scenarioName,
                   @JsonProperty("expectedStates") final List<State> expectedStates)
    {
        this.scenarioName = scenarioName;
        this.expectedStates = expectedStates;
    }

    public String getScenarioName()
    {
        return scenarioName;
    }

    public List<State> getExpectedStates()
    {
        return expectedStates;
    }
}
