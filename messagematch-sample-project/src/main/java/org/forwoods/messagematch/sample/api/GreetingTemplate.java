package org.forwoods.messagematch.sample.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GreetingTemplate {
    private final String language;
    private final String greetingTemplate;

    public GreetingTemplate(@JsonProperty("language") String language, @JsonProperty("greetingTemplate") String greetingTemplate) {
        this.language = language;
        this.greetingTemplate = greetingTemplate;
    }

    public String getLanguage() {
        return language;
    }

    public String getGreetingTemplate() {
        return greetingTemplate;
    }
}
