package org.forwoods.messagematch.spec;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ThreadPoolExecutor;

public class CallExample {
    //Can be a reference to an example elsewhere
    private URL reference;
    private String relative;

    //or an inline example
    private Channel channel;
    private JsonNode requestMessage;
    private JsonNode responseMessage;
    @JsonProperty("schema") private URL verifySchema;

    public void resolve(URL base) {
        try {
            if (channel==null) {
                CallExample remote;
                if (relative!=null){
                    reference = new URL(base, relative);
                }
                remote = TestSpec.specParser.readValue(reference, CallExample.class);
                this.channel = remote.channel;
                this.requestMessage = remote.requestMessage;
                this.responseMessage = remote.responseMessage;
                this.verifySchema = remote.verifySchema;
            }
        } catch (IOException e) {
            throw new RuntimeException("Error resolving refered spec", e);
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public JsonNode getRequestMessage() {
        return requestMessage;
    }

    public JsonNode getResponseMessage() {
        return responseMessage;
    }

    public URL getVerifySchema() {
        return verifySchema;
    }

    public URL getReference() {
        return reference;
    }

    public String getRelative() {
        return relative;
    }
}
