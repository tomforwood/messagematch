package org.forwoods.messagematch.server.model.compatibility;

import lombok.*;
import org.forwoods.messagematch.server.model.VersionedArtifact;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class VersionTestResult {
    private VersionedArtifact testedAgainst;
    private TestResult result;
}
