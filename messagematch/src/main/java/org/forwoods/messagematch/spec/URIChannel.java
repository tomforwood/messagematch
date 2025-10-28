package org.forwoods.messagematch.spec;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("uri")
public class URIChannel implements Channel{
    private final String path;
    private final String method;
    private final int statusCode;
    private final String statusLine;
    public URIChannel(@JsonProperty("path") String path,
                      @JsonProperty("method") String method,
                      @JsonProperty("statusCode") final int statusCode,
                      @JsonProperty("statusLine") final String statusLine) {
        this.path = path;
        this.method = method;
        this.statusCode = statusCode;
        this.statusLine = statusLine;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public int getStatusCode()
    {
        return statusCode;
    }

    public String getStatusLine()
    {
        return statusLine;
    }

    @Override
    public String toString()
    {
        return "URIChannel{" +
                "path='" + path + '\'' +
                ", method='" + method + '\'' +
                ", statusCode=" + statusCode +
                ", statusLine='" + statusLine + '\'' +
                '}';
    }
}
