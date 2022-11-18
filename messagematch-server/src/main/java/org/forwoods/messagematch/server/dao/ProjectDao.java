package org.forwoods.messagematch.server.dao;

import org.forwoods.messagematch.server.model.Artifact;
import org.forwoods.messagematch.server.model.project.MonitoredProject;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectDao extends CrudRepository<MonitoredProject, Long> {

    <S extends MonitoredProject> S findByArtifact(Artifact artifact);
}
