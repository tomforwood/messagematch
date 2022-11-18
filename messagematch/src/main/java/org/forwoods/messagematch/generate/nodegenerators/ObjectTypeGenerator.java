package org.forwoods.messagematch.generate.nodegenerators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

public class ObjectTypeGenerator extends NodeGenerator{
    final Map<String, NodeGenerator> children = new LinkedHashMap<>();
    public ObjectTypeGenerator() {
        super(null);
    }

    @Override
    public JsonNode generate() {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<String, NodeGenerator> child:children.entrySet()) {
            node.set(child.getKey(), child.getValue().generate());
        }
        return node;
    }

    public void addChild(String name, NodeGenerator node) {
        children.put(name, node);
    }

}
