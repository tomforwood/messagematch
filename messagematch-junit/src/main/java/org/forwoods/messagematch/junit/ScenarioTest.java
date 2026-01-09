package org.forwoods.messagematch.junit;

import java.time.Instant;

public record ScenarioTest(boolean result, String api, String testName, String scenarioName, String scenarioHash, String apiVersion, Instant runtime) {
}
