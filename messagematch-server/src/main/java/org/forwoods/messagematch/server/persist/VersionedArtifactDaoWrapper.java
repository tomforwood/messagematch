package org.forwoods.messagematch.server.persist;

import org.forwoods.messagematch.server.dao.VersionedArtifactDao;
import org.forwoods.messagematch.server.model.Artifact;
import org.forwoods.messagematch.server.model.VersionedArtifact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.StreamSupport;

@Component
public class VersionedArtifactDaoWrapper {
    @Autowired
    VersionedArtifactDao versionedArtifactDao;
    @Autowired
    ArtifactDaoWrapper artifactDaoWrapper;

    public <S extends VersionedArtifact> S save(S entity) {
        entity.setArtifact(artifactDaoWrapper.save(entity.getArtifact()));//replace the artifact with a canonical one
        S result = findByArtifactAndVersion(entity.getArtifact(), entity.getVersion());
        if (result!=null) {
            return result;
        }
        else {
            return versionedArtifactDao.save(entity);
        }
    }

    public <S extends VersionedArtifact> S findByArtifactAndVersion(Artifact artifact, String version) {
        return versionedArtifactDao.findByArtifactAndVersion(artifact, version);
    }

    public <S extends VersionedArtifact> Iterable<S> saveAll(Iterable<S> entities) {
        return StreamSupport.stream(entities.spliterator(), false).map(this::save).toList();
    }
}
