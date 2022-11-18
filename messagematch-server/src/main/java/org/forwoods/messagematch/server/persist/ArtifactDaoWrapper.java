package org.forwoods.messagematch.server.persist;

import org.forwoods.messagematch.server.dao.ArtifactDao;
import org.forwoods.messagematch.server.model.Artifact;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.util.stream.StreamSupport;

@Component
public class ArtifactDaoWrapper {

    @Autowired
    ArtifactDao artifactDao;

    public <S extends Artifact> S findByGroupIdAndArtifactId(String groupId, String artifactId) {
        return artifactDao.findByGroupIdAndArtifactId(groupId, artifactId);
    }

    public <S extends Artifact> @Nonnull S save(@Nonnull S entity) {
        //Check to see if it exists already
        S result = findByGroupIdAndArtifactId(entity.getGroupId(), entity.getArtifactId());
        if (result!=null) return result;
        return artifactDao.save(entity);
    }

    public <S extends Artifact> @Nonnull Iterable<S> saveAll(Iterable<S> entities) {
        return StreamSupport.stream(entities.spliterator(), false).map(this::save).toList();
    }


}
