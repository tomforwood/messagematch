package org.forwoods.messagematch.apiscenario;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.forwoods.messagematch.apiscenario.spec.APITestScenario;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioMatcherTest {


    @Test
    public void shouldStoreAndRetrieveAnEntity() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream in = ScenarioMatcherTest.class.getResourceAsStream("/entityCreate.apiScenario");

        APITestScenario scenario = mapper.readValue(in, APITestScenario.class);
        ScenarioMatcher matcher = new ScenarioMatcher(scenario);

        final ScenarioMatcher.HttpResponse put = matcher.findMatchHttp("/5/storedNumbers", "[\"7\"]", "POST");
        assertThat(put).isNotNull();
        final int id = put.responseBody().get("id").asInt();

        final ScenarioMatcher.HttpResponse get = matcher.findMatchHttp("/5/storedNumbers/" + (id), null, "GET");
        assertThat(get).isNotNull();
        final int anInt = get.responseBody().get(0).asInt();
        assertThat(anInt).isEqualTo(7);


    }

    @Test
    public void shouldHaveBoundInGenerate() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        InputStream in = ScenarioMatcherTest.class.getResourceAsStream("/entityCreate.apiScenario");

        APITestScenario scenario = mapper.readValue(in, APITestScenario.class);
        ScenarioMatcher matcher = new ScenarioMatcher(scenario);

        final ScenarioMatcher.HttpResponse put = matcher.findMatchHttp("/5/storedNumbers", "[\"5\"]", "POST");
        assertThat(put).isNotNull();
        final int id = put.responseBody().get("id").asInt();

        assertThat(matcher.findMatchHttp("/5/storedNumbers/"+(id+1), null, "GET")).isNull();

    }

}