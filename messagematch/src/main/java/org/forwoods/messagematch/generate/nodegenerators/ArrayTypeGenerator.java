package org.forwoods.messagematch.generate.nodegenerators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.ArrayList;
import java.util.List;

public class ArrayTypeGenerator extends NodeGenerator{
    List<NodeGenerator> children = new ArrayList<>();
    public ArrayTypeGenerator() {
        super(null);
    }

    @Override
    public JsonNode generate() {
        ArrayNode node = JsonNodeFactory.instance.arrayNode();
        for (NodeGenerator child:children) {
            node.add(child.generate());
        }
        return node;
    }

    public void addChild(NodeGenerator node) {
        children.add(node);
    }
}
