package org.forwoods.messagematch.server.service;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.forwoods.messagematch.server.model.VersionedArtifact;
import org.forwoods.messagematch.server.model.project.MonitoredProject;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class GitService {
    public void checkoutProject(String environment, VersionedArtifact artifact, MonitoredProject project, Path cloneDir) throws GitAPIException {
        try (Git git = Git.cloneRepository().setURI(project.getUri())
                .setDirectory(cloneDir.toFile())
                //.setCredentialsProvider()
                .call()) {

            String branchName = project.getEnvironmentBranches().get(environment);
            if (branchName == null) {
                git.checkout().setName("refs/tags/" + artifact.getVersion()).call();
            } else {
                git.checkout().setName("refs/branches/" + branchName).call();
            }
        }
    }
}
