package org.forwoods.messagematch.server.dao;

import org.forwoods.messagematch.server.model.Artifact;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtifactDao extends CrudRepository<Artifact, Long> {
    <S extends Artifact> S findByGroupIdAndArtifactId(String groupId, String artifactId);
}
