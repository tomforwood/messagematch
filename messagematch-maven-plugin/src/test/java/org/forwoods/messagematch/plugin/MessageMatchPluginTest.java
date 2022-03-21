package org.forwoods.messagematch.plugin;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.mockito.Mockito.*;

class MessageMatchPluginTest {

    MessageMatchPlugin plugin;
    Log log;
    @BeforeEach
    public void setup() {
        log = mock(Log.class);

        plugin = new MessageMatchPlugin();
        plugin.setLog(log);
    }


    @Test
    public void testAllSpecsRun() throws MojoExecutionException {
        Resource res = new Resource();
        res.setDirectory("src/test/resources/successs");
        plugin.setResourceDirs(List.of(res));
        plugin.setTimestampString("2022-02-17T16:00:03Z[Europe/London]");
        plugin.verifyMessageMatches();
        verifyNoInteractions(log);
    }

    @Test
    public void testSpecsMissed() throws MojoExecutionException {
        Resource res = new Resource();
        res.setDirectory("src/test/resources");
        plugin.setResourceDirs(List.of(res));
        plugin.setTimestampString("2022-02-17T16:00:03Z[Europe/London]");
        plugin.verifyMessageMatches();
        String content = "src/test/resources/fails/fish.testSpec has not been checked with a test"
        		.replace('/',File.separatorChar);
		verify(log).warn(content);

    }

    @Test
    public void testAllSwaggerAPISTested() throws MojoExecutionException {
        Resource res = new Resource();
        res.setDirectory("src/test/resources/success");
        plugin.setResourceDirs(List.of(res));
        plugin.setTimestampString("2022-02-17T16:00:03Z[Europe/London]");
        plugin.setOpenApiFiles(List.of("src/test/resources/success/testApi.json"));
        plugin.verifyMessageMatches();
        verifyNoInteractions(log);
    }

    @Test
    public void testSwaggerAPIMissed() throws MojoExecutionException {
        Resource res = new Resource();
        res.setDirectory("src/test/resources/fails");
        plugin.setResourceDirs(List.of(res));
        plugin.setTimestampString("2022-02-17T16:00:03Z[Europe/London]");
        plugin.setOpenApiFiles(List.of("src/test/resources/fails/testApi.json"));

        plugin.verifyMessageMatches();
        verify(log).warn("API path of GET:/users does not match any tested channel");
        verify(log).warn("API path of GET:/user/{userId} does not match any tested channel");
    }

}