package org.forwoods.messagematch.server.service;

import org.forwoods.messagematch.server.dao.OverallRecordDao;
import org.forwoods.messagematch.server.model.VersionedArtifact;
import org.forwoods.messagematch.server.model.compatibility.OverallTestRecord;
import org.forwoods.messagematch.server.model.compatibility.TestRecord;
import org.forwoods.messagematch.server.model.compatibility.TestResult;
import org.forwoods.messagematch.server.persist.TestRecordDaoWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class TestRecordService {

    @Autowired
    TestRecordDaoWrapper recordDao;
    @Autowired
    OverallRecordDao overallRecordDao;

    public void setSuccessful(VersionedArtifact artifactUnderTest, Iterable<? extends VersionedArtifact> testedAgainst) {
        for (VersionedArtifact artifactTestedAgainst : testedAgainst) {
            Optional<TestRecord> existing = recordDao.findByArtifactUnderTestAndArtifactTestedAgainst(artifactUnderTest, artifactTestedAgainst);
            existing.ifPresentOrElse(tr -> {
                if (tr.getResult() != TestResult.SUCCEEDED) {
                    tr.setResult(TestResult.SUCCEEDED);
                    recordDao.save(tr);
                }
            }, () -> {
                TestRecord newRecord = new TestRecord();
                newRecord.setArtifactUnderTest(artifactUnderTest);
                newRecord.setArtifactTestedAgainst(artifactTestedAgainst);
                newRecord.setResult(TestResult.SUCCEEDED);
                recordDao.save(newRecord);
            });
        }
    }

    public void setFailed(VersionedArtifact artifactUnderTest, Iterable<? extends VersionedArtifact> testedAgainst) {
        for (VersionedArtifact artifactTestedAgainst:testedAgainst) {
            Optional<TestRecord> existing = recordDao.findByArtifactUnderTestAndArtifactTestedAgainst(artifactUnderTest, artifactTestedAgainst);
            //If there was an existing record it was either already a failure and we needn't do anything
            //OR it was a success and the failure was down to some other artifact
            if (existing.isEmpty()) {
                TestRecord newRecord = new TestRecord();
                newRecord.setArtifactUnderTest(artifactUnderTest);
                newRecord.setArtifactTestedAgainst(artifactTestedAgainst);
                newRecord.setResult(TestResult.FAILED);
                recordDao.save(newRecord);
            }

        }
    }

    public void saveOverallTestResult(VersionedArtifact builtArtifact, boolean result) {
        OverallTestRecord overallTestRecord = new OverallTestRecord();
        overallTestRecord.setArtifact(builtArtifact);
        overallTestRecord.setResult(result?TestResult.SUCCEEDED: TestResult.FAILED);
        overallRecordDao.save(overallTestRecord);
    }


}
