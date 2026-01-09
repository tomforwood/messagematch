package org.forwoods.messagematch.reporting.api;

import java.time.Instant;

public record ApiVersion(String versionTag, Instant lastTestTime) {
}
