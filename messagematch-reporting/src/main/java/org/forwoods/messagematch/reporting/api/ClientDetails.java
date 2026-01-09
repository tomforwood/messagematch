package org.forwoods.messagematch.reporting.api;

import java.util.List;

public record ClientDetails(String name, List<String> versions) {
}
