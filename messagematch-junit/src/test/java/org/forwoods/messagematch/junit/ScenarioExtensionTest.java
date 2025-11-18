package org.forwoods.messagematch.junit;

import org.forwoods.messagematch.apiscenario.spec.APITestScenario;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ScenarioExtension.class)
public class ScenarioExtensionTest {

    @Test
    public void testLoad(@Scenario("entityCreate.apiScenario") APITestScenario scenario) {
        assertTrue(scenario !=null);
    }

    @AfterAll
    public static void testLastRunFile() throws IOException {

    }

    @Test
    public void testOther() {
        System.out.println("other");
    }
}