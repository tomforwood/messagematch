package org.forwoods.messagematch.server.persist;

import org.forwoods.messagematch.server.dao.TestRecordDao;
import org.forwoods.messagematch.server.model.VersionedArtifact;
import org.forwoods.messagematch.server.model.compatibility.TestRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class TestRecordDaoWrapper {
    @Autowired
    TestRecordDao recordDao;

    public <S extends TestRecord> S save(S test) {
        Optional<TestRecord> existing = recordDao.findByArtifactUnderTestAndArtifactTestedAgainst(test.getArtifactUnderTest(), test.getArtifactTestedAgainst());
        return existing.map(tr->{
            test.setId(tr.getId());
            return recordDao.save(test);
        }).orElse(recordDao.save(test));
    }

    public Optional<TestRecord> findByArtifactUnderTestAndArtifactTestedAgainst(VersionedArtifact artifactUnderTest, VersionedArtifact artifactTestedAgainst) {
        return recordDao.findByArtifactUnderTestAndArtifactTestedAgainst(artifactUnderTest, artifactTestedAgainst);
    }

    public <S extends TestRecord> List<S> findByArtifactUnderTest(VersionedArtifact artifactUnderTest) {
        return recordDao.findByArtifactUnderTest(artifactUnderTest);
    }

    public void saveAll(List<TestRecord> testRecords) {
        testRecords.forEach(this::save);
    }
}
