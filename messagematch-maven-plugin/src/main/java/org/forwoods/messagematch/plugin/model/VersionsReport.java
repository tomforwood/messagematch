package org.forwoods.messagematch.plugin.model;

import org.apache.maven.artifact.Artifact;

import java.util.Set;

public class VersionsReport {
    private Artifact builtArtifact;
    private Set<Artifact> specDependencies;
    private String deployedEnvironment;

    public Artifact getBuiltArtifact() {
        return builtArtifact;
    }

    public void setBuiltArtifact(Artifact builtArtifact) {
        this.builtArtifact = builtArtifact;
    }

    public Set<Artifact> getSpecDependencies() {
        return specDependencies;
    }

    public void setSpecDependencies(Set<Artifact> specDependencies) {
        this.specDependencies = specDependencies;
    }

    public void setDeployedEnvironment(String deployedEnvironment) {
        this.deployedEnvironment = deployedEnvironment;
    }
}
