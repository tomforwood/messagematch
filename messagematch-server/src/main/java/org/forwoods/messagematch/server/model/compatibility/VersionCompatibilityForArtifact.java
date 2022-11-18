package org.forwoods.messagematch.server.model.compatibility;

import lombok.Getter;
import lombok.Setter;
import org.forwoods.messagematch.server.model.VersionedArtifact;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class VersionCompatibilityForArtifact {
    private VersionedArtifact versionedArtifactTested;
    private TestResult overallResult;
    private List<VersionTestResult> results = new ArrayList<>();

    public VersionCompatibilityForArtifact(VersionedArtifact versionedArtifactTested) {
        this.versionedArtifactTested = versionedArtifactTested;
    }

    public VersionCompatibilityForArtifact(){}

    @Override
    public String toString() {
        return "VersionCompatibilityForArtifact{" + "versionedArtifactTested=" + versionedArtifactTested +
                ", overallResult=" + overallResult +
                ", results=" + results +
                '}';
    }
}
