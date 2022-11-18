package org.forwoods.messagematch.server.service;

import org.apache.maven.shared.invoker.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.forwoods.messagematch.server.config.MessagematchServerConfig;
import org.forwoods.messagematch.server.dao.VersionDeploymentDao;
import org.forwoods.messagematch.server.model.Artifact;
import org.forwoods.messagematch.server.model.VersionedArtifact;
import org.forwoods.messagematch.server.model.deployed.VersionsDeployedReport;
import org.forwoods.messagematch.server.model.project.MonitoredProject;
import org.forwoods.messagematch.server.persist.ProjectDaoWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MavenInvokerService {

    @Autowired
    VersionDeploymentDao versionsDao;

    @Autowired
    ProjectDaoWrapper projectDAO;

    @Autowired
    GitService gitService;

    @Autowired
    MessagematchServerConfig config;

    @Autowired
    TestRecordService testRecords;

    public boolean runAgainstDeployedVersions(String environment, VersionedArtifact artifact) throws GitAPIException, IOException, MavenInvocationException {
        //Find what artifacts this artifact depends on
        VersionsDeployedReport deployedReport = versionsDao.findByBuiltArtifact(artifact);


        //build the map of the latest versions deployed of each artifact
        Collection<VersionsDeployedReport> latestVersions = versionsDao.findLatestVersionsIn(environment);
        var versionsToUse = latestVersions.stream()
                .map(VersionsDeployedReport::getBuiltArtifact)
                .filter(v->uses(deployedReport, v))
                .collect(Collectors.toMap(VersionedArtifact::getArtifact, v->v));

        Path buildDir = rewritePom(environment, artifact, versionsToUse);


        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File(config.getMavenHome()));
        InvocationRequest request = new DefaultInvocationRequest();
        request.setBaseDirectory(buildDir.toFile());
        request.setGoals(List.of("test"));
        InvocationResult invocationResult = invoker.execute(request);

        boolean success = 0==invocationResult.getExitCode();

        if (success) {
            testRecords.setSuccessful(artifact, versionsToUse.values());
        }
        else {
           testRecords.setFailed(artifact, versionsToUse.values());
        }
        testRecords.saveOverallTestResult(artifact, success);

        return success;
    }

    private Path rewritePom(String environment, VersionedArtifact artifact, Map<Artifact, VersionedArtifact> versionsToUse) throws IOException, GitAPIException {
        //get the project details
        MonitoredProject project = projectDAO.findByArtifact(artifact.getArtifact());

        //check out the tag
        Path cloneDir = Files.createTempDirectory(project.getArtifact().getArtifactId());

        gitService.checkoutProject(environment, artifact, project, cloneDir);

        //manipulate the poms

        //find them all and rewrite each one
        try (Stream<Path> walk = Files.walk(cloneDir)){
            walk.filter(f -> f.getFileName().toString().equals("pom.xml")).forEach(f -> {rewrite(f, versionsToUse);
                try {
                    String s = Files.readString(f);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return cloneDir;
        }
    }


    protected void rewrite(Path f, Map<Artifact, VersionedArtifact> versionsToUse) {
        try {
            Document pom = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().parse(Files.newInputStream(f));
            XPath xpath = XPathFactory.newDefaultInstance().newXPath();
            NodeList nodes = (NodeList) xpath.evaluate("/project/dependencies/dependency", pom,
                    XPathConstants.NODESET);
            for (int i = 0;i<nodes.getLength();i++) {
                Node dependency = nodes.item(i);
                VersionedArtifact versionToUse = matching(dependency, versionsToUse);
                if (versionToUse!=null) {
                    setVersion(dependency, versionToUse.getVersion());
                }
            }

            Transformer transformer = TransformerFactory.newDefaultInstance().newTransformer();
            DOMSource source = new DOMSource(pom);
            StreamResult result = new StreamResult(Files.newOutputStream(f));
            transformer.transform(source, result);

        } catch (SAXException | IOException | ParserConfigurationException | XPathExpressionException |
                 TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    private void setVersion(Node dependency, String version) {
        NodeList children = dependency.getChildNodes();
        for (int i=0;i< children.getLength();i++) {
            Node child = children.item(i);
            if ("version".equals(child.getNodeName())){
                child.setTextContent(version);
            }
        }
    }

    private VersionedArtifact matching(Node dependency, Map<Artifact, VersionedArtifact> versionsToUse) {
        NodeList children = dependency.getChildNodes();
        Artifact dependencyArtifact = new Artifact();
        for (int i=0;i< children.getLength();i++) {
            Node child = children.item(i);
            if ("groupId".equals(child.getNodeName())){
                dependencyArtifact.setGroupId(child.getTextContent());
            }
            if ("artifactId".equals(child.getNodeName())) {
                dependencyArtifact.setArtifactId(child.getTextContent());
            }
        }

        return versionsToUse.get(dependencyArtifact);
    }

    private boolean uses(VersionsDeployedReport deployedReport, VersionedArtifact v) {
        return deployedReport.getSpecDependencies().stream().map(VersionedArtifact::getArtifact).anyMatch(a->a.equals(v.getArtifact()));
    }

}
