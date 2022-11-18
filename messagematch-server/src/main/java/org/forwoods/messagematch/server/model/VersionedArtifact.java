package org.forwoods.messagematch.server.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Setter
@Getter
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { "artifact_id", "version" }) })
public class VersionedArtifact {
        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        long id;

        @ManyToOne(cascade = {CascadeType.MERGE})
        @JoinColumn(name = "artifact_id")
        private Artifact artifact;
        private String version;

        @Override
        public String toString() {
                return "VersionedArtifact{" + "artifact=" + artifact +
                        ", version='" + version + '\'' +
                        '}';
        }
}
