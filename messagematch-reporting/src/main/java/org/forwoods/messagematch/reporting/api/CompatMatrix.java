package org.forwoods.messagematch.reporting.api;

import java.util.List;

public record CompatMatrix(List<ApiCompatibilities> apis, List<String> untestedScenarios) {

}


