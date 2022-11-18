package org.forwoods.messagematch.generate.nodegenerators;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A sligtly hacky way to pass back the signal that the first entry in this array is a special
 * value that specifies the required size of the array rather than an actual node
 */
public class ArraySizeGenerator extends NodeGenerator{
    final Integer min;
    final Integer max;
    public ArraySizeGenerator(Integer min, Integer max) {
        super(null);
        this.min = min;
        this.max = max;
    }

    @Override
    public JsonNode generate() {
        return null;
    }

    public Integer getMin() {
        return min;
    }

    public Integer getMax() {
        return max;
    }
}
