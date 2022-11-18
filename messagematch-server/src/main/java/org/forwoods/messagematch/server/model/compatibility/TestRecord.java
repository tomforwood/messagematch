package org.forwoods.messagematch.server.model.compatibility;

import lombok.Getter;
import lombok.Setter;
import org.forwoods.messagematch.server.model.VersionedArtifact;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "TestRecord", uniqueConstraints = {
        @UniqueConstraint(name = "uc_testrecord", columnNames = {"artifactUnderTest_id", "artifactTestedAgainst_id"})
})
@Getter
@Setter
public class TestRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(cascade = {CascadeType.MERGE})
    @JoinColumn(name = "artifactUnderTest_id")
    VersionedArtifact artifactUnderTest;
    @ManyToOne(cascade = {CascadeType.MERGE})
    @JoinColumn(name = "artifactTestedAgainst_id")
    VersionedArtifact artifactTestedAgainst;
    TestResult result;
    Instant timestamp = Instant.now();
}
