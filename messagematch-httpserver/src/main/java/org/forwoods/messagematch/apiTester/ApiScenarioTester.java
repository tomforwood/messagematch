package org.forwoods.messagematch.apiTester;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.forwoods.messagematch.apiscenario.spec.APITestScenario;
import org.forwoods.messagematch.generate.JsonGenerator;
import org.forwoods.messagematch.generate.PathGenerator;
import org.forwoods.messagematch.match.JsonMatcher;
import org.forwoods.messagematch.spec.CallExample;
import org.forwoods.messagematch.spec.URIChannel;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * This class attempts to execute all the the calls contained in a api test scenario
 * It is designed to be run against a real instance of the api and ensure that the
 * behaviour specified in the scenario matches the real behaior of the API
 */
public class ApiScenarioTester
{
    private final APITestScenario apiTestScenario;
    private final URI baseUri;
    private final Map<String, String> headers;
    private final CloseableHttpClient httpClient;

    public ApiScenarioTester(final APITestScenario apiTestScenario, URI baseUri)
    {
        this(apiTestScenario, baseUri, Map.of());
    }

    public ApiScenarioTester(final APITestScenario apiTestScenario, URI baseUri, final Map<String, String> headers)
    {
        this.apiTestScenario = apiTestScenario;
        this.baseUri = baseUri;
        this.headers = headers;
        httpClient = HttpClients.createDefault();
    }

    public void executeTestScenario(final Map<String, Object> bindings)
    {
        apiTestScenario.getExpectedStates().forEach(state -> {
            if (state.getStateTrigger()!=null)
            {
                executeCall(state.getStateTrigger(), bindings);
            }
            state.getExpectedCalls().forEach(expectedCall -> executeCall(expectedCall, bindings));
        });
    }

    private void executeCall(final CallExample<URIChannel> callUnderTest, final Map<String, Object> bindings)
    {
        final PathGenerator pathGenerator = new PathGenerator(callUnderTest.getChannel().getPath(), bindings);
        String path = pathGenerator.generate();
        //bindings.putAll(pathGenerator.); //TODO extract the bindings from here
        URI uri = baseUri.resolve(path);
        try
        {
            HttpUriRequest request = switch (callUnderTest.getChannel().getMethod())
            {
                case "GET" -> new HttpGet(uri);
                case "PUT" ->
                {
                    final HttpPut httpPut = new HttpPut(uri);
                    setBody(httpPut, callUnderTest.getRequestMessage(), bindings);
                    yield httpPut;
                }
                case "DELETE" -> new HttpDelete(uri);
                case "POST" ->
                {
                    final HttpPost httpPost = new HttpPost(uri);
                    setBody(httpPost, callUnderTest.getRequestMessage(), bindings);
                    yield httpPost;
                }
                default ->
                        throw new IllegalStateException("Unexpected value: " + callUnderTest.getChannel().getMethod());
            };
            headers.forEach(request::setHeader);
            final CloseableHttpResponse httpResponse = httpClient.execute(request);
            if (callUnderTest.getChannel().getStatusCode() > 0 && httpResponse.getStatusLine().getStatusCode() != callUnderTest.getChannel().getStatusCode())
            {
                fail("Call to " + callUnderTest.getChannel().getMethod() + " " + path + " returned statuscode "+ httpResponse.getStatusLine().getStatusCode() + " expected "+callUnderTest.getChannel().getStatusCode());
            }
            if (callUnderTest.getChannel().getStatusLine() != null && !httpResponse.getStatusLine().getReasonPhrase().equals(callUnderTest.getChannel().getStatusLine()))
            {
                fail("Call to " + callUnderTest.getChannel().getMethod() + " " + path + " return statusMessage "+ httpResponse.getStatusLine().getReasonPhrase() + " expected "+callUnderTest.getChannel().getStatusLine());
            }
            if (callUnderTest.getResponseMessage()!=null) {
                JsonMatcher matcher = new JsonMatcher(callUnderTest.getResponseMessage(), EntityUtils.toString(httpResponse.getEntity()));
                matcher.getBindings().putAll(bindings);
                if (!matcher.matches()) {
                    fail("Call to " + path + " returned a body that didn't match");
                }

                bindings.putAll(matcher.getBindings());
            }

        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void setBody(final HttpEntityEnclosingRequestBase httpRequest, final JsonNode requestMessage, final Map<String, Object> bindings) throws UnsupportedEncodingException
    {
        JsonGenerator generator = new JsonGenerator(requestMessage, bindings);
        final String text = generator.generate().toString();
        //TODO the bindings need to back propogate
        httpRequest.setEntity(new StringEntity(text));
    }
}
