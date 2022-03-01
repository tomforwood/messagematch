package org.forwoods.messagematch.plugin;

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
    public void testAllSpecsRun() throws MojoExecutionException, MojoFailureException {
        plugin.setResourceDir(new File("src/test/resources/success"));
        plugin.setTimestampString("2022-02-17T16:00:03Z[Europe/London]");
        plugin.execute();
        verifyNoInteractions(log);
    }

    @Test
    public void testSpecsMissed() throws MojoExecutionException, MojoFailureException {
        plugin.setResourceDir(new File("src/test/resources"));
        plugin.setTimestampString("2022-02-17T16:00:03Z[Europe/London]");
        plugin.execute();
        String content = "src/test/resources/fails/fish.testSpec has not been checked with a test";
        content = content.replace('/',File.separatorChar);
		verify(log).warn(content);

    }

    @Test
    public void testAllSwaggerAPISTested() throws MojoExecutionException, MojoFailureException {
        plugin.setResourceDir(new File("src/test/resources/success"));
        plugin.setTimestampString("2022-02-17T16:00:03Z[Europe/London]");
        plugin.setOpenApiFiles(List.of("src/test/resources/success/testApi.json"));
        plugin.execute();
        verifyNoInteractions(log);
    }

    @Test
    public void testSwaggerAPIMissed() {

    }

}