package org.forwoods.messagematch.sample2.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Greeting {
    private final String greeting;

    public Greeting(@JsonProperty("greeting") String greeting) {
        this.greeting = greeting;
    }

    public String getGreeting() {
        return greeting;
    }
}
