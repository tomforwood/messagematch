package org.forwoods.messagematch.plugin;

import org.forwoods.messagematch.junit.MessageSpecExtension;
import org.forwoods.messagematch.spec.TestSpec;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.forwoods.messagematch.spec.TestSpec.TEST_SPEC;

@Mojo(name = "message-api-validate", defaultPhase = LifecyclePhase.VERIFY)
public class MessageMatchPlugin extends AbstractMojo {

    @Parameter(defaultValue = "${project.testResources[0].directory}", required = true, readonly = true)
    File resourceDir;

    @Parameter(defaultValue = "${session.request.startTime}", readonly = true)
    private String timestampString;

    @Parameter(readonly = true)
    private List<String> openApiFiles;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Path resourcePath= resourceDir.toPath();
        if (!Files.exists(resourcePath)) return;
        //System.out.println(timestampDate);
        Instant start = ZonedDateTime.parse(timestampString).toInstant();
        try {
            Map<Path, Instant> lastRun = Files.walk(resourcePath).filter(p->p.toString().endsWith(TEST_SPEC))
                    .collect(Collectors.toMap(p->p, p->getLastRunTime(p)));
            lastRun.entrySet().stream().filter(e->e.getValue()==null || e.getValue().isBefore(start)).forEach(e->{
                getLog().warn(e.getKey() +  " has not been checked with a test");
            });

            if (openApiFiles!=null && !openApiFiles.isEmpty()) {
                List<TestSpec> specs = lastRun.keySet().stream().map(p->readSpec(p)).collect(Collectors.toList());
                MessageMatchSwaggerChecker checker = new MessageMatchSwaggerChecker();
                openApiFiles.stream().map(f->Path.of(f)).forEach(f->checker.checkOpenApi(specs, f, getLog()));
            }


        }
        catch (IOException|RuntimeException e) {
            throw new MojoExecutionException(e);
        }
    }



    private TestSpec readSpec(Path p) {
        try (InputStream is = Files.newInputStream(p)) {
            TestSpec testSpec = TestSpec.specParser.readValue(is, TestSpec.class);
            testSpec.resolve(p.toUri().toURL());
            return testSpec;
        }
        catch (IOException e) {
            return null;
        }
    }

    private Instant getLastRunTime(Path p) {
        try {
            Path dir = p.getParent();
            String n = p.getFileName().toString();
            n = "." + n.replace(TEST_SPEC, MessageSpecExtension.LAST_USED);
            Path lastUsedPath = dir.resolve(n);
            if (!Files.exists(lastUsedPath)) return Instant.EPOCH;
            String content = Files.readString(lastUsedPath);
            System.out.println(content);
            return ZonedDateTime.parse(content).toInstant();
        } catch (IOException e) {
            return Instant.EPOCH;
        }
    }

    public void setResourceDir(File baseDir) {
        this.resourceDir = baseDir;
    }

    public void setTimestampString(String timestampString) {
        this.timestampString = timestampString;
    }

    public void setOpenApiFiles(List<String> openApiFiles) {
        this.openApiFiles = openApiFiles;
    }
}
