package org.forwoods.messagematch.plugin;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MessageMatchPluginTest {

    MessageMatchPlugin plugin;
    Log log;
    @BeforeEach
    public void setup() {
        log = mock(Log.class,i-> {System.out.println(i.getArgument(0).toString());return null;});

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
        //plugin.setMessageMatchServer(URI.create("testURI"));
        //HttpClient client = mock(HttpClient.class);
        //when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(null);
        //plugin.setHttpClient(client);

        plugin.runValidations();
        verifyNoInteractions(log);
        //ArgumentCaptor<HttpRequest> cap = ArgumentCaptor.forClass(HttpRequest.class);
        //verify(client).send(cap.capture(), any(HttpResponse.BodyHandler.class));
    }

    @Test
    public void testSpecsMissed() throws MojoExecutionException {
        Resource res = new Resource();
        res.setDirectory("src/test/resources/fails");
        plugin.setResourceDirs(List.of(res));
        plugin.setTimestampString("2022-02-17T16:00:03Z[Europe/London]");
        plugin.verifyMessageMatches();
        String content = "src/test/resources/fails/fish.testSpec has not been checked with a test"
        		.replace('/',File.separatorChar);
		verify(log).error(content);

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

    @Test
    public void testSwaggerAPIMissedButExcluded() throws MojoExecutionException {
        Resource res = new Resource();
        res.setDirectory("src/test/resources/fails");
        plugin.setResourceDirs(List.of(res));
        plugin.setTimestampString("2022-02-17T16:00:03Z[Europe/London]");
        plugin.setOpenApiFiles(List.of("src/test/resources/fails/testApi.json"));
        plugin.setExcludePaths(List.of("/user/**"));

        plugin.verifyMessageMatches();
        verify(log, never()).warn("API path of GET:/user/{userId} does not match any tested channel");
    }

    @Test
    public void testSwaggerAPIMismatch() {
        Resource res = new Resource();
        res.setDirectory("src/test/resources/mismatch");
        plugin.setResourceDirs(List.of(res));
        plugin.setTimestampString("2022-02-17T16:00:03Z[Europe/London]");

        assertThrows(MojoExecutionException.class, ()->plugin.verifyMessageMatches());
        verify(log).error("call with channel get:/user/abc and request body null did not match anything in the specified schema classpath:mismatch/testApi.json see debug for things it nearly matched with");
        verify(log).error("call with channel get:/user/123 and request body null did not match anything in the specified schema classpath:mismatch/testApi.json see debug for things it nearly matched with");
    }

    @Test
    public void testSwaggerAPIMismatchLevelOverride() throws MojoExecutionException {
        Resource res = new Resource();
        res.setDirectory("src/test/resources/mismatch");
        plugin.setResourceDirs(List.of(res));
        plugin.overrideValidationLevels(Map.of("UNUSED_SPEC", "WARN", "UNTESTED_ENDPOINT", "WARN", "MISSMATCHED_SPEC", "WARN"));
        plugin.setTimestampString("2022-02-17T16:00:03Z[Europe/London]");

        plugin.verifyMessageMatches();
        verify(log).warn("call with channel get:/user/abc and request body null did not match anything in the specified schema classpath:mismatch/testApi.json see debug for things it nearly matched with");
        verify(log).warn("call with channel get:/user/123 and request body null did not match anything in the specified schema classpath:mismatch/testApi.json see debug for things it nearly matched with");
    }

}