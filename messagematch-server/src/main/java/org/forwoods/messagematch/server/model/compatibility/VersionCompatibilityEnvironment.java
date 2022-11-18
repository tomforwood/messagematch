package org.forwoods.messagematch.server.model.compatibility;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class VersionCompatibilityEnvironment {

    private String environment;
    private List<VersionCompatibilityForArtifact> versionCompatibilities = new ArrayList<>();

    public VersionCompatibilityEnvironment(String environmentName) {
        environment = environmentName;
    }
    public VersionCompatibilityEnvironment(){}

    @Override
    public String toString() {
        return "VersionCompatibilityEnvironment{" +
                "environment='" + environment + '\'' +
                ", versionCompatibilities=" + versionCompatibilities +
                '}';
    }
}
