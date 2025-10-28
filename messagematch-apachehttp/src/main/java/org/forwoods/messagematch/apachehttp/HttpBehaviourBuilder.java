package org.forwoods.messagematch.apachehttp;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.forwoods.messagematch.junit.BehaviourBuilder;
import org.forwoods.messagematch.match.JsonMatcher;
import org.forwoods.messagematch.spec.*;
import org.mockito.ArgumentMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HttpBehaviourBuilder extends BehaviourBuilder<URIChannel> {


    @Override
    protected void addFilteredBehavior(Stream<TriggeredCall<URIChannel>> calls) {
        HttpClient httpClient = getHttpClient();
        calls.map(TriggeredCall::getCall).forEach(c-> {
            try {
                when(httpClient.execute(argThat(new RequestMatcher(c)))).thenAnswer(new HttpAnswer(c.getResponseMessage()));
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
        public HttpResponse answer(InvocationOnMock invocation) throws Throwable {
            StringEntity entity = new StringEntity(responseMessage.toString());
            BasicHttpResponse basicHttpResponse = new BasicHttpResponse(new BasicStatusLine(HttpVersion.HTTP_1_1, 200, ""));
            basicHttpResponse.setEntity(entity);
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
            switch (channel.getMethod()) {
                case "get":
                    HttpGet get = (HttpGet) argument;
                    URI u = get.getURI();
                    if (!u.getPath().equals(channel.getPath())) return false;
                    URIBuilder builder = new URIBuilder(u);
                    List<NameValuePair> queryParams = builder.getQueryParams();
                    Map<String, Object> map = queryParams.stream()
                            .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
                    JsonNode nodeParams = TestSpec.specParser.valueToTree(map);
                    JsonMatcher matcher = new JsonMatcher(call.getRequestMessage(), nodeParams);
                    return matcher.matches();

                case "post" :
                    HttpPost post = (HttpPost) argument;
                    u = post.getURI();
                    if (!u.getPath().equals(channel.getPath())) return false;
                    try {
                        matcher = new JsonMatcher(call.getRequestMessage(),TestSpec.specParser.readTree(post.getEntity().getContent()));
                        return matcher.matches();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                default:{
                    throw new UnsupportedOperationException("method "+channel.getMethod() + " not currently supported");
                }
            }

        }
    }
}
