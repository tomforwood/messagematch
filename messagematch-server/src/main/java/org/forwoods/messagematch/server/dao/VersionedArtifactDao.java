package org.forwoods.messagematch.server.dao;

import org.forwoods.messagematch.server.model.Artifact;
import org.forwoods.messagematch.server.model.VersionedArtifact;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VersionedArtifactDao extends CrudRepository<VersionedArtifact, Long> {
    <S extends VersionedArtifact> S findByArtifactAndVersion(Artifact artifact, String version);
}
