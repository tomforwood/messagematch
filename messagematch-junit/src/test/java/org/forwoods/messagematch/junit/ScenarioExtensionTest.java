package org.forwoods.messagematch.junit;

import org.forwoods.messagematch.apiscenario.spec.APITestScenario;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(ScenarioExtension.class)
public class ScenarioExtensionTest {

    @Test
    public void testLoad(@Scenario("scentest.apiScenario") APITestScenario scenario) {
        assertNotNull(scenario);
    }

    @AfterAll
    public static void testLastRunFile() {

    }

    @Test
    public void testOther() {
        System.out.println("other");
    }
}