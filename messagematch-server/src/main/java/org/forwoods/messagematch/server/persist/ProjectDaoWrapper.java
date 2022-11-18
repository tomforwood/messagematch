package org.forwoods.messagematch.server.persist;

import org.forwoods.messagematch.server.dao.ProjectDao;
import org.forwoods.messagematch.server.model.Artifact;
import org.forwoods.messagematch.server.model.project.MonitoredProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectDaoWrapper {

    @Autowired
    ProjectDao projectDao;

    @Autowired
    ArtifactDaoWrapper artifactDao;

    public <S extends MonitoredProject> S findByArtifact(Artifact artifact) {
        return projectDao.findByArtifact(artifact);
    }

    public <S extends MonitoredProject> S save(S entity) {
        entity.setArtifact(artifactDao.save(entity.getArtifact()));
        S result = findByArtifact(entity.getArtifact());
        if (result != null) {
            //will now do an update
            entity.setId(result.getId());
        }
        return projectDao.save(entity);
    }
}
