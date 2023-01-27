package org.forwoods.messagematch.util;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class PathExtractor {

    public static JsonNode extractPrimitiveNode(String matcher, Map<String, Object> bindings) {
        int slashIndex = matcher.indexOf('/');
        String objName;
        String jsonPath = "/";
        if (slashIndex > 0) {
            objName = matcher.substring(4, slashIndex);
            jsonPath = matcher.substring(slashIndex);
        }
        else {
            objName = matcher.substring(4);
        }
        Object fromBinding = bindings.get(objName);
        if (fromBinding==null) return null;
        return ((JsonNode) fromBinding).at(jsonPath);
    }

    public static JsonNode extractObjectNode(String matcher, Map<String, Object> bindings) {
        int slashIndex = matcher.indexOf('/');
        String objName;
        String jsonPath;
        if (slashIndex > 0) {
            objName = matcher.substring(0, slashIndex);
            jsonPath = matcher.substring(slashIndex);
        }
        else {
            objName = matcher;
            jsonPath = "";
        }
        Object fromBinding = bindings.get(objName);
        if (fromBinding==null) return null;
        return ((JsonNode) fromBinding).at(jsonPath);
    }
}
