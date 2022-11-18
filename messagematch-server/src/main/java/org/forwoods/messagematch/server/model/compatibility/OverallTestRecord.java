package org.forwoods.messagematch.server.model.compatibility;

import lombok.Getter;
import lombok.Setter;
import org.forwoods.messagematch.server.model.VersionedArtifact;

import javax.persistence.*;

@Entity
@Getter
@Setter
public class OverallTestRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToOne
    VersionedArtifact artifact;
    TestResult result;
}
