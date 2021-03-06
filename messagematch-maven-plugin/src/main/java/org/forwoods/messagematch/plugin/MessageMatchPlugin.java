package org.forwoods.messagematch.plugin;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.FileSet;
import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.forwoods.messagematch.junit.MessageSpecExtension;
import org.forwoods.messagematch.spec.TestSpec;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.forwoods.messagematch.spec.TriggeredCall;
import org.forwoods.messagematch.util.ClasspathURLStreamHandlerProvider;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.forwoods.messagematch.spec.TestSpec.TEST_SPEC;

@Mojo(name = "message-api-validate", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.TEST)
public class MessageMatchPlugin extends AbstractMojo {

    @Parameter(defaultValue = "${project.testResources}", required = true, readonly = true)
    List<Resource> resourceDirs;

    @Parameter(required = true, readonly = true)
    private String timestampString;

    @Parameter(readonly = true)
    private List<String> openApiFiles;

    @Parameter(readonly = true)
    private List<String> channelClasses;

    @Parameter(readonly = true)
    private List<String> excludePaths;



    private final Map<Validation, Level> actualValidationLevels = Arrays.stream(Validation.values()).collect(Collectors.toMap(v->v, Validation::getDefault));
    @Parameter(readonly = true, property = "validationLevels")
    private Map<String, String> validationLevels;

    protected void overrideValidationLevels(Map<String, String> validationLevels) {
        for (Map.Entry<String, String> override:validationLevels.entrySet()) {
            Validation v = Validation.parse(override.getKey());
            Level l = Level.parse(override.getValue());
            if (v!=null && l!=null) {
                getLog().debug("overriding "+v + " with "+l);
                this.actualValidationLevels.put(v,l);
            }
        }
    }


    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    private ClassLoader buildClassPath() {
        try {
            Set<URL> urls = new HashSet<>();
            @SuppressWarnings("unchecked")
            List<String> elements = project.getTestClasspathElements();
            //getRuntimeClasspathElements()
            //getCompileClasspathElements()
            //getSystemClasspathElements()
            for (String element : elements) {
                urls.add(new File(element).toURI().toURL());
            }

            return URLClassLoader.newInstance(
                    urls.toArray(new URL[0]),
                    Thread.currentThread().getContextClassLoader());


        } catch (MalformedURLException | DependencyResolutionRequiredException  e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void execute() throws MojoExecutionException {

        ClassLoader cl = buildClassPath();
        Thread.currentThread().setContextClassLoader(cl);


        if (channelClasses!=null) {
            channelClasses.forEach(c->{try {
                cl.loadClass(c);
                Class.forName(c, true, cl);
            } catch (ClassNotFoundException e) {
                getLog().error(e);
            }
            });
        }

        if (validationLevels!=null) overrideValidationLevels(validationLevels);

        try {
            URL.setURLStreamHandlerFactory(new ClasspathURLStreamHandlerProvider());
        }
        catch (Error ignored){
            getLog().info("Error adding classpath URL handler - it may have been previously added. " +
                    "If there is an error about resolving later this could be the cause");
        }
        verifyMessageMatches();
    }

    protected void verifyMessageMatches() throws MojoExecutionException {
        List<Path> resourcePaths= resourceDirs.stream().map(FileSet::getDirectory)
                .map(Path::of)
                .filter(Files::exists).collect(Collectors.toList());
        Instant start = ZonedDateTime.parse(timestampString).toInstant();
        try {
            boolean buildPassed;
            Map<Path, Instant> lastRun = new HashMap<>();
            resourcePaths.forEach(f -> {
                try {
                    Map<Path, Instant> temp = Files.walk(f).filter(p -> p.toString().endsWith(TEST_SPEC))
                            .collect(Collectors.toMap(p -> p, this::getLastRunTime));
                    lastRun.putAll(temp);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            buildPassed = lastRun.entrySet().stream().filter(e -> e.getValue() == null || e.getValue().isBefore(start))
                    .map(e -> {
                        Level l = actualValidationLevels.get(Validation.UNUSED_SPEC);
                        l.log(getLog(), e.getKey() + " has not been checked with a test");
                        return l != Level.FAIL;
                    })
                    .reduce(true, Boolean::logicalAnd);


            List<TestSpec> specs = lastRun.keySet().stream().map(this::readSpec).filter(Objects::nonNull).collect(Collectors.toList());
            MessageMatchSwaggerChecker checker = new MessageMatchSwaggerChecker(getLog(), actualValidationLevels);
            buildPassed &= specs.stream().flatMap(c->Stream.concat(Stream.of(c.getCallUnderTest()),c.getSideEffects().stream().map(TriggeredCall::getCall)))
                    .filter(s->s.getVerifySchema()!=null).map(checker::checkOpenpi).reduce(true, Boolean::logicalAnd);

            if (openApiFiles!=null && !openApiFiles.isEmpty()) {
                buildPassed &= openApiFiles.stream().map(Path::of).map(f->checker.checkOpenApi(specs, f, excludePaths)).reduce(true, Boolean::logicalAnd);
            }

            if (!buildPassed) {
                throw new MojoExecutionException("MessageMatch api validation failed - see log for details");
            }


        }
        catch (RuntimeException e) {
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
            return ZonedDateTime.parse(content).toInstant();
        } catch (IOException e) {
            return Instant.EPOCH;
        }
    }

    public void setResourceDirs(List<Resource> baseDirs) {
        this.resourceDirs = baseDirs;
    }

    public void setTimestampString(String timestampString) {
        this.timestampString = timestampString;
    }

    public void setOpenApiFiles(List<String> openApiFiles) {
        this.openApiFiles = openApiFiles;
    }

    public void setChannelClasses(List<String> channelClasses) {
        this.channelClasses = channelClasses;
    }

    public void setProject(MavenProject project) {
        this.project = project;
    }

    public void setExcludedPaths(List<String> excludedPaths) {
        this.excludePaths = excludedPaths;
    }

    public void setValidationLevels(Map<String, String> levels) {
        this.validationLevels = levels;
    }
}
