package org.forwoods.messagematch.generate.nodegenerators;

import com.fasterxml.jackson.databind.JsonNode;

public class ReferenceGenerator extends NodeGenerator{

    private final JsonNode value;

    public ReferenceGenerator(JsonNode value) {
        super(null);
        this.value = value;
    }

    @Override
    public JsonNode generate() {
        return value;
    }
}
