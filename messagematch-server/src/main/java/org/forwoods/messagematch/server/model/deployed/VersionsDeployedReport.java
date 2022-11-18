package org.forwoods.messagematch.server.model.deployed;


import org.forwoods.messagematch.server.model.VersionedArtifact;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity
public class VersionsDeployedReport {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public long id;

    public Instant reportTime;

    @OneToOne(cascade = {CascadeType.MERGE})
    @JoinColumn(name = "built_artifact_id")
    VersionedArtifact builtArtifact;
    @ManyToMany(cascade = {CascadeType.MERGE})
    List<VersionedArtifact> specDependencies;
    public String deployedEnvironment;

    public VersionedArtifact getBuiltArtifact() {
        return builtArtifact;
    }

    public void setBuiltArtifact(VersionedArtifact builtVersionedArtifact) {
        this.builtArtifact = builtVersionedArtifact;
    }

    public List<VersionedArtifact> getSpecDependencies() {
        return specDependencies;
    }

    public void setSpecDependencies(List<VersionedArtifact> specDependencies) {
        this.specDependencies = specDependencies;
    }

    public Instant getReportTime() {
        return reportTime;
    }

    public String getDeployedEnvironment() {
        return deployedEnvironment;
    }
}
