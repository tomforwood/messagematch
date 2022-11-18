package org.forwoods.messagematch.apachehttp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.forwoods.messagematch.spec.TriggeredCall;
import org.forwoods.messagematch.spec.URIChannel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class HttpBehaviourBuilderTest {

    @Test
    public void testHttpBehaviour() throws IOException, URISyntaxException {
        HttpClient client = mock(HttpClient.class, Mockito.RETURNS_SMART_NULLS);
        HttpBehaviourBuilder behave = new HttpBehaviourBuilder();
        behave.addMocks(Map.of(HttpClient.class, client));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode request = mapper.readTree("{\"id\":1}");
        JsonNode responseNode = mapper.readTree("\"Hello tom\"");

        TriggeredCall<URIChannel> call = new TriggeredCall<>(null, null,
                null, null,
                new URIChannel("/hello_world", "get"),
                request,
                responseNode,
                null);
        behave.addBehavior(List.of(call));

        URI u = new URI("http://localhost/hello_world?id=1");
        HttpGet get = new HttpGet(u);
        HttpResponse response = client.execute(get);
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String l=reader.lines().collect(Collectors.joining());

        assertEquals("\"Hello tom\"", l);
    }

}