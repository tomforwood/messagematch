package org.forwoods.messagematch.server.service;

import com.google.common.jimfs.Jimfs;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.forwoods.messagematch.server.config.MessagematchServerConfig;
import org.forwoods.messagematch.server.dao.VersionDeploymentDao;
import org.forwoods.messagematch.server.model.Artifact;
import org.forwoods.messagematch.server.model.VersionedArtifact;
import org.forwoods.messagematch.server.model.deployed.VersionsDeployedReport;
import org.forwoods.messagematch.server.model.project.MonitoredProject;
import org.forwoods.messagematch.server.persist.ProjectDaoWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.hamcrest.MockitoHamcrest;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MavenInvokerServiceTest {



    @Mock
    GitService gitService;
    @Mock
    VersionDeploymentDao versionsDao;
    @Mock
    ProjectDaoWrapper projectDao;

    @Mock
    TestRecordService recordService;

    @InjectMocks
    final
    MavenInvokerService invoker = new MavenInvokerService();

    @BeforeEach
    public void setup() throws IOException {
        MessagematchServerConfig config = new MessagematchServerConfig();
        Process p = Runtime.getRuntime().exec("where mvn");
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String s = r.readLine();
        s=s.trim();
        System.out.println("path is ["+s+"]");
        Path mvnExec = Path.of(s);
        s=mvnExec.getParent().getParent().toString();
        config.setMavenHome(s);
        invoker.config = config;
    }

    @Test
    void testMavenRewrite() throws IOException {
        try (FileSystem fs = Jimfs.newFileSystem()) {
            Path pomFilePath = fs.getPath("pom.xml");
            Files.copy(this.getClass().getResourceAsStream("/pom-rewrite/before-pom.xml"), pomFilePath);

            Artifact sample1 = new Artifact();
            sample1.setArtifactId("messagematch-sample-project");
            sample1.setGroupId("org.forwoods");
            VersionedArtifact v1 = new VersionedArtifact();
            v1.setArtifact(sample1);
            v1.setVersion("0.1.5");
            Map<Artifact, VersionedArtifact> versions = Map.of(sample1, v1);
            invoker.rewrite(pomFilePath, versions);
            String result = Files.readString(pomFilePath);

            assertThat(result).isEqualTo(contentOf(this.getClass().getResource("/pom-rewrite/after-pom.xml")));
        }
    }

    @Test
    void testProjectBuild() throws GitAPIException, MavenInvocationException, IOException {
        Artifact sample2 = new Artifact();
        sample2.setArtifactId("messagematch-sample-project2");
        sample2.setGroupId("org.forwoods");
        VersionedArtifact versioned2 = new VersionedArtifact();
        versioned2.setArtifact(sample2);
        versioned2.setVersion("0.1.5");

        Artifact sample1 = new Artifact();
        sample1.setArtifactId("messagematch-sample-project");
        sample1.setGroupId("org.forwoods");
        VersionedArtifact sample1Versioned = new VersionedArtifact();
        sample1Versioned.setArtifact(sample2);
        sample1Versioned.setVersion("0.1.5");

        VersionsDeployedReport sample2Deployed = new VersionsDeployedReport();
        sample2Deployed.setBuiltArtifact(versioned2);
        sample2Deployed.setSpecDependencies(List.of(sample1Versioned));

        MonitoredProject project = new MonitoredProject();
        project.setArtifact(sample2);


        when(projectDao.findByArtifact(sample2)).thenReturn(project);

        when(versionsDao.findByBuiltArtifact(versioned2)).thenReturn(sample2Deployed);
        VersionsDeployedReport sample1Deployed = new VersionsDeployedReport();
        sample1Deployed.setBuiltArtifact(sample1Versioned);
        when(versionsDao.findLatestVersionsIn("qa")).thenReturn(List.of(sample1Deployed));


        doAnswer(a->{
            Path path = a.getArgument(3);
            String zipFileName = "/git-repo/messagematch-sample-project2.zip";
            extract(zipFileName, path);
            return null;})
                .when(gitService).checkoutProject(eq("qa"), eq(versioned2), eq(project), any(Path.class));

        invoker.runAgainstDeployedVersions("qa", versioned2);
        verify(recordService, times(1)).setSuccessful(eq(versioned2), MockitoHamcrest.argThat(contains(sample1Versioned)));
        verify(recordService, times(1)).saveOverallTestResult(versioned2, true);
    }

    @Test
    void testProjectBuildFails() throws GitAPIException, MavenInvocationException, IOException {
        Artifact sample2 = new Artifact();
        sample2.setArtifactId("messagematch-sample-project2");
        sample2.setGroupId("org.forwoods");
        VersionedArtifact versioned2 = new VersionedArtifact();
        versioned2.setArtifact(sample2);
        versioned2.setVersion("0.1.5");

        Artifact sample1 = new Artifact();
        sample1.setArtifactId("messagematch-sample-project");
        sample1.setGroupId("org.forwoods");
        VersionedArtifact sample1Versioned = new VersionedArtifact();
        sample1Versioned.setArtifact(sample1);
        sample1Versioned.setVersion("0.0.1");//This version didn't exist - build will fail

        VersionsDeployedReport sample2Deployed = new VersionsDeployedReport();
        sample2Deployed.setBuiltArtifact(versioned2);
        sample2Deployed.setSpecDependencies(List.of(sample1Versioned));

        MonitoredProject project = new MonitoredProject();
        project.setArtifact(sample2);


        when(projectDao.findByArtifact(sample2)).thenReturn(project);

        when(versionsDao.findByBuiltArtifact(versioned2)).thenReturn(sample2Deployed);
        VersionsDeployedReport sample1Deployed = new VersionsDeployedReport();
        sample1Deployed.setBuiltArtifact(sample1Versioned);
        when(versionsDao.findLatestVersionsIn("qa")).thenReturn(List.of(sample1Deployed));

        doAnswer(a->{
            Path path = a.getArgument(3);
            String zipFileName = "/git-repo/messagematch-sample-project2.zip";
            extract(zipFileName, path);
            return null;})
                .when(gitService).checkoutProject(eq("qa"), eq(versioned2), eq(project), any(Path.class));

        boolean result = invoker.runAgainstDeployedVersions("qa", versioned2);
        assertThat(result).isFalse();
        verify(recordService, times(1)).setFailed(eq(versioned2), MockitoHamcrest.argThat(contains(sample1Versioned)));
        verify(recordService, times(1)).saveOverallTestResult(versioned2, false);
    }

    private static void extract(String zipFileName, Path destination) {
        try (InputStream stream = MavenInvokerServiceTest.class.getResourceAsStream(zipFileName)){
            if(stream==null) {
                throw new RuntimeException("Cannot open "+zipFileName);
            }
            ZipInputStream zis = new ZipInputStream(stream);
            ZipEntry entry;
            while ((entry = zis.getNextEntry())!=null) {
                if (entry.isDirectory()) continue;
                Path dest = destination.resolve(entry.getName());
                Files.createDirectories(dest.getParent());
                zis.transferTo(Files.newOutputStream(dest));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}