package org.forwoods.messagematch.apiTester;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import org.forwoods.messagematch.apiscenario.spec.APITestScenario;
import org.forwoods.messagematch.junit.Scenario;
import org.forwoods.messagematch.junit.ScenarioExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URI;
import java.util.HashMap;

/**
 * This is an example of a Scenario test
 * For your real system you would create a number of test classes that execute
 * different scenarios against your API
 */
@ExtendWith(ScenarioExtension.class)
public class ApiScenarioTesterTest
{
    URI baseUri;

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
    public void testWorkingSpec(@Scenario("entityCreate.apiScenario") APITestScenario scenario)
    {
        //if this were a real service we were testing we would nee to do some setup
        //to put it into a state where this test will pass
        //e.g. create the "account" that the scenario is going to use
        ApiScenarioTester tester = new ApiScenarioTester(scenario, baseUri);
        tester.executeTestScenario(new HashMap<>());
    }

    /*
    This test does not in fact pass - the NumberServer cannot store negative numbers
    It will be recorded in the test database as a failure
     */
    @Test
    public void testBrokenSpec(@Scenario("numberStoreNegative.apiScenario") APITestScenario scenario)
    {
        //Yuo would not expect to write a failing scenario
        ApiScenarioTester tester = new ApiScenarioTester(scenario, baseUri);
        tester.executeTestScenario(new HashMap<>());
    }

    @Test
    public void testCantStoreNegativeNumbers(@Scenario("numberStoreNegativeFails.apiScenario") APITestScenario scenario)
    {
        ApiScenarioTester tester = new ApiScenarioTester(scenario, baseUri);
        tester.executeTestScenario(new HashMap<>());
    }
}
