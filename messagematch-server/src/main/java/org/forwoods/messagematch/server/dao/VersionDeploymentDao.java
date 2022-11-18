package org.forwoods.messagematch.server.dao;

import org.forwoods.messagematch.server.model.VersionedArtifact;
import org.forwoods.messagematch.server.model.deployed.VersionsDeployedReport;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface VersionDeploymentDao extends CrudRepository<VersionsDeployedReport, Long>
{
    VersionsDeployedReport findByBuiltArtifact(VersionedArtifact versionedArtifact);

    @Query("select v from VersionsDeployedReport v where v.deployedEnvironment in ?1 and v.reportTime = " +
            "(select max(reportTime) from VersionsDeployedReport v2 where v.builtArtifact=v2.builtArtifact" +
            " and v.deployedEnvironment=v2.deployedEnvironment)")
    List<VersionsDeployedReport> findLatestForEnvironments(Collection<String> deployedEnvironments);
    default List<VersionsDeployedReport> findLatestVersionsIn(String deployedEnvironment) {
        return findLatestForEnvironments(List.of(deployedEnvironment));
    }

}
