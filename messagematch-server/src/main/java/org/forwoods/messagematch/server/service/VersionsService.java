package org.forwoods.messagematch.server.service;

import com.google.common.collect.Multimap;
import org.forwoods.messagematch.server.config.MessagematchServerConfig;
import org.forwoods.messagematch.server.model.VersionedArtifact;
import org.forwoods.messagematch.server.model.compatibility.*;
import org.forwoods.messagematch.server.model.deployed.VersionsDeployedReport;
import org.forwoods.messagematch.server.persist.TestRecordDaoWrapper;
import org.forwoods.messagematch.server.persist.VersionDeploymentDaoWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VersionsService {

    @Autowired
    MessagematchServerConfig config;

    @Autowired
    VersionDeploymentDaoWrapper versionDeploymentDaoWrapper;

    @Autowired
    TestRecordDaoWrapper recordDao;

    public void saveVersionsReport(VersionsDeployedReport report) {
            versionDeploymentDaoWrapper.save(report);
            var testRecords = report.getSpecDependencies().stream()
                    .map(s->{TestRecord record = new TestRecord();
                        record.setResult(TestResult.SUCCEEDED);
                        record.setArtifactUnderTest(report.getBuiltArtifact());
                        record.setArtifactTestedAgainst(s);
                        record.setTimestamp(Instant.now());
                    return record;}).toList();
            recordDao.saveAll(testRecords);
    }

    public VersionCompatibilityReport getVersionCompatibilities() {

        VersionCompatibilityReport result = new VersionCompatibilityReport();

        //Get the latest version of each monitored artifact in each environment
        Multimap<String, VersionsDeployedReport> latestVersions = versionDeploymentDaoWrapper.getLatestArtifactVersions(config.getMonitoredEnvironments());

        //for each environment we are monitoring
        for (var environmentName:config.getMonitoredEnvironments()) {
            VersionCompatibilityEnvironment environment = new VersionCompatibilityEnvironment(environmentName);
            var reports = latestVersions.get(environmentName);
            //build a map to provide the latest deployment of each artifact
            var latestVersionMap = reports.stream().collect(Collectors.toMap(v -> v.getBuiltArtifact().getArtifact(), VersionsDeployedReport::getBuiltArtifact));

            //for each service that deployed in this environment
            for (var rep:reports) {
                VersionedArtifact builtArtifact = rep.getBuiltArtifact();
                VersionCompatibilityForArtifact forArtifact = new VersionCompatibilityForArtifact(builtArtifact);
                List<VersionedArtifact> specDependencies = rep.getSpecDependencies();
                TestResult overall = TestResult.SUCCEEDED;
                //For each dependency the aftifact depends on
                for (var dependency:specDependencies) {
                    //get the latest deployed version of that
                    var latestVersionOfDependency = latestVersionMap.get(dependency.getArtifact());
                    //and find out if we have tested against that version
                    var testResult = recordDao.findByArtifactUnderTestAndArtifactTestedAgainst(builtArtifact, latestVersionOfDependency)
                            .map(TestRecord::getResult).orElse(TestResult.UNTESTED);
                    VersionTestResult vtr = new VersionTestResult(latestVersionOfDependency, testResult);
                    forArtifact.getResults().add(vtr);
                    overall = overall.merge(testResult);
                }
                forArtifact.setOverallResult(overall);
                environment.getVersionCompatibilities().add(forArtifact);
            }
            result.getEnvironments().add(environment);
        }

        return result;
    }
}
