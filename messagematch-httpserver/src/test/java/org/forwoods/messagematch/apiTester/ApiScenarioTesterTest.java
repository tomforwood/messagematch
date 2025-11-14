package org.forwoods.messagematch.apiTester;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.forwoods.messagematch.apiscenario.spec.APITestScenario;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApiScenarioTesterTest
{
    static List<String> specFiles = List.of("entityCreate.apiScenario", "numberStoreNegative.apiScenario", "numberStoreNegativeFails.apiScenario");
    static Map<String, APITestScenario> specs = new HashMap<>();
    URI baseUri;


    @BeforeAll
    public static void setupAll()
    {

        for (final String specFile : specFiles)
        {
            try (InputStream fin = ApiScenarioTesterTest.class.getClassLoader().getResourceAsStream(specFile))
            {
                ObjectMapper mapper = new ObjectMapper();
                final APITestScenario apiSpec = mapper.readValue(fin, APITestScenario.class);
                specs.put(specFile, apiSpec);

            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    @BeforeEach
    public void setupEach()
    {
        System.out.println("beforeEach");
        final Vertx vertx = Vertx.vertx();
        Future<NumberStoreServer> applicationFuture = NumberStoreServer.deploy(vertx);
        applicationFuture.onComplete(ar ->
                baseUri = URI.create("http://localhost:" + ar.result().getPort()));
        applicationFuture.await();
    }

    @Test
    public void testWorkingSpec()
    {
        //doSetup
        //e.g. create the "account" that the scenario is going to use

        //do scenario
        APITestScenario spec = specs.get("entityCreate.apiScenario");
        ApiScenarioTester tester = new ApiScenarioTester(spec, baseUri);
        tester.executeTestScenario(new HashMap<>());
    }

    @Test
    public void testBrokenSpec()
    {
        APITestScenario spec = specs.get("numberStoreNegative.apiScenario");
        ApiScenarioTester tester = new ApiScenarioTester(spec, baseUri);
        tester.executeTestScenario(new HashMap<>());
        //assertThrows(AssertionFailedError.class, ()->tester.executeTestScenario(new HashMap<>()), "Should have failed");
    }

    @Test
    public void testCantStoreNegatveNumbers()
    {
        APITestScenario spec = specs.get("numberStoreNegativeFails.apiScenario");
        ApiScenarioTester tester = new ApiScenarioTester(spec, baseUri);
        tester.executeTestScenario(new HashMap<>());
        //assertThrows(AssertionFailedError.class, ()->tester.executeTestScenario(new HashMap<>()), "Should have failed");
    }
}
