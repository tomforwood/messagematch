package org.forwoods.messagematch.server.persist;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.forwoods.messagematch.server.dao.VersionDeploymentDao;
import org.forwoods.messagematch.server.model.deployed.VersionsDeployedReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class VersionDeploymentDaoWrapper {

    @Autowired
    VersionDeploymentDao versionDeploymentDao;
    @Autowired
    VersionedArtifactDaoWrapper versionedArtifactDaoWrappper;

    public <S extends VersionsDeployedReport> S save(S entity) {
        entity.setBuiltArtifact(versionedArtifactDaoWrappper.save(entity.getBuiltArtifact()));//replace build artifact with canonical
        entity.setSpecDependencies(entity.getSpecDependencies().stream().map(v-> versionedArtifactDaoWrappper.save(v)).toList());//and all the dependencies
        return versionDeploymentDao.save(entity);
    }

    public Multimap<String, VersionsDeployedReport> getLatestArtifactVersions(List<String> monitoredEnvironments) {
        Iterable<VersionsDeployedReport> all = versionDeploymentDao.findAll();
        return Multimaps.index(versionDeploymentDao.findLatestForEnvironments(monitoredEnvironments), VersionsDeployedReport::getDeployedEnvironment);
    }
}