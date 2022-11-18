package org.forwoods.messagematch.server.dao;

import org.forwoods.messagematch.server.model.VersionedArtifact;
import org.forwoods.messagematch.server.model.compatibility.TestRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestRecordDao extends CrudRepository<TestRecord, Long> {

    Optional<TestRecord> findByArtifactUnderTestAndArtifactTestedAgainst(VersionedArtifact artifactUnderTest, VersionedArtifact artifactTestedAgainst);

    <S extends TestRecord> List<S> findByArtifactUnderTest(VersionedArtifact artifactUnderTest);
}
