package org.forwoods.messagematch.junit;

import java.time.Instant;

public record SuccessfulScenarioTest(String testName, String scenarioName, String scenarioHash, String apiVersion, Instant runtime) {
}
