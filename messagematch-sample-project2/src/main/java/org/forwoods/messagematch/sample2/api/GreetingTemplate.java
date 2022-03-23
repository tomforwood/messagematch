package org.forwoods.messagematch.sample2.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GreetingTemplate {
    private final String language;
    private final String greebingTemplate;

    public GreetingTemplate(@JsonProperty("language") String language, @JsonProperty("greebingTemplate") String greebingTemplate) {
        this.language = language;
        this.greebingTemplate = greebingTemplate;
    }

    public String getLanguage() {
        return language;
    }

    public String getGreebingTemplate() {
        return greebingTemplate;
    }
}
