package org.forwoods.messagematch.spec;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("uri")
public class URIChannel implements Channel{
    final String uri;
    final String method;
    public URIChannel(@JsonProperty("uri") String uri,
                      @JsonProperty("method") String method) {
        this.uri = uri;
        this.method = method;
    }

    public String getUri() {
        return uri;
    }

    public String getMethod() {
        return method;
    }

    @Override
    public String toString() {
        return method+ ":"+uri;
    }
}
