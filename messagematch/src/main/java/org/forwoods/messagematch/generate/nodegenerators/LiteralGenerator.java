package org.forwoods.messagematch.generate.nodegenerators;

import com.fasterxml.jackson.databind.JsonNode;

public class LiteralGenerator extends NodeGenerator {

    private final JsonNode node;

    public LiteralGenerator(JsonNode node) {
        super(null);
        this.node = node;
    }
    @Override
    public JsonNode generate() {
        return node;
    }
}
