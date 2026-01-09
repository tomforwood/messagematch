package org.forwoods.messagematch.reporting.api;

import java.util.List;
import java.util.Map;

public record ApiCompatibilities(String name, List<ApiVersion> allVersions, Map<String, ApiTestVersions> scenariosTestedByVersions){

}
