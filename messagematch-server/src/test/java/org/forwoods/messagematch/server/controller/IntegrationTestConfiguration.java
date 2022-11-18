package org.forwoods.messagematch.server.controller;


import org.forwoods.messagematch.server.dao.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@ComponentScan("org.forwoods.messagematch.server.controller")
@ComponentScan("org.forwoods.messagematch.server.service")
@ComponentScan("org.forwoods.messagematch.server.dao")
@Import(org.forwoods.messagematch.server.integration.IntegrationTestConfiguration.class)
@TestConfiguration
public class IntegrationTestConfiguration {

    @MockBean
    VersionDeploymentDao versionsDeployedDao;

    @MockBean
    VersionedArtifactDao versionsArtifactDao;

    @MockBean
    ArtifactDao artifactDao;

    @MockBean
    TestRecordDao recordDao;

    @MockBean
    ProjectDao projectDao;

    @MockBean
    OverallRecordDao overallRecordDao;



}
