package org.forwoods.messagematch.apachehttp;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.net.URIBuilder;
import org.forwoods.messagematch.junit.BehaviourBuilder;
import org.forwoods.messagematch.match.JsonMatcher;
import org.forwoods.messagematch.spec.*;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HttpBehaviourBuilder extends BehaviourBuilder<URIChannel> {


    @Override
    protected void addFilteredBehavior(Stream<TriggeredCall<URIChannel>> calls) {
        HttpClient httpClient = getHttpClient();
        calls.map(TriggeredCall::getCall).forEach(c-> {
            try {
                //noinspection deprecation
                when(httpClient.execute(argThat(new RequestMatcher(c)))).thenAnswer(new HttpAnswer(c.getResponseMessage()));
                when(httpClient.execute(argThat(new RequestMatcher(c)), any(HttpClientResponseHandler.class))).thenReturn(c.getResponseMessage().toString());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    //TODO switch to new verification design
    public void verifyBehaviour(Collection<TriggeredCall<?>> calls) {
        HttpClient httpClient = getHttpClient();
        filteredCalls(calls).filter(TriggeredCall::hasTimes).forEach(c-> {
            try {
                //noinspection deprecation
                verify(httpClient).execute(argThat(new RequestMatcher(c.getCall())));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    protected Class<URIChannel> getChannelType() {
        return URIChannel.class;
    }

    private HttpClient getHttpClient() {
        return (HttpClient) this.mocks.entrySet().stream()
                .filter(e->HttpClient.class.isAssignableFrom(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst().orElseThrow(()->new RuntimeException("Mock implementing apache HttpClient not found"));
    }

    private static class HttpAnswer implements Answer<HttpResponse> {

        private final JsonNode responseMessage;

        public HttpAnswer(JsonNode responseMessage) {
            this.responseMessage = responseMessage;
        }

        @Override
        public HttpResponse answer(InvocationOnMock invocation) {
            SimpleHttpResponse basicHttpResponse = new SimpleHttpResponse(200, "");
            basicHttpResponse.setBody(responseMessage.toString().getBytes(StandardCharsets.UTF_8), ContentType.APPLICATION_JSON);
            return basicHttpResponse;
        }
    }

    private static class RequestMatcher implements ArgumentMatcher<HttpUriRequest> {
        private final CallExample<URIChannel> call;
        private final URIChannel channel;
        public RequestMatcher(CallExample<URIChannel> c) {
            this.call=c;
            this.channel=c.getChannel();
        }

        @Override
        public boolean matches(HttpUriRequest argument) {
            try {
                URI u;
                JsonMatcher matcher;
                switch (channel.getMethod()) {
                    case "get":
                        HttpGet get = (HttpGet) argument;
                        u = get.getUri();
                        if (!u.getPath().equals(channel.getPath())) return false;
                        URIBuilder builder = new URIBuilder(u);
                        List<NameValuePair> queryParams = builder.getQueryParams();
                        Map<String, Object> map = queryParams.stream()
                                .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
                        JsonNode nodeParams = TestSpec.specParser.valueToTree(map);
                        matcher = new JsonMatcher(call.getRequestMessage(), nodeParams);
                        return matcher.matches();

                    case "post":
                        HttpPost post = (HttpPost) argument;
                        u = post.getUri();
                        if (!u.getPath().equals(channel.getPath())) return false;
                        try {
                            matcher = new JsonMatcher(call.getRequestMessage(), TestSpec.specParser.readTree(post.getEntity().getContent()));
                            return matcher.matches();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    default: {
                        throw new UnsupportedOperationException("method " + channel.getMethod() + " not currently supported");
                    }
                }
            }
            catch (URISyntaxException ex)
            {
                throw new RuntimeException(ex);
            }
        }
    }
}
