package org.forwoods.messagematch.generate.nodegenerators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.ArrayList;
import java.util.List;

public class ArrayTypeGenerator extends NodeGenerator{
    final List<NodeGenerator> children = new ArrayList<>();
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

    public void setSize(Integer min, Integer max) {
        //if the size is less than specified duplicate teh last element until big enough
        //if it is greater - throw exception
       if (min!=null && children.size()<min) {
           int additinalRequired = min-children.size();
           NodeGenerator toAdd = children.get(children.size()-1);
           for (int i=0;i<=additinalRequired;i++) {
               addChild(toAdd);
           }
       }
       if (max!=null && children.size()>max) {
           throw new IllegalStateException("max size of array is specified as "+ max + " but there are " + children.size() + " elements specified");
       }


    }
}
