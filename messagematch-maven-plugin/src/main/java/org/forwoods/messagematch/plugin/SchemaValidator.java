package org.forwoods.messagematch.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import io.swagger.v3.oas.models.media.*;
import org.forwoods.messagematch.match.JsonPath;
import org.forwoods.messagematch.match.MatchError;
import org.forwoods.messagematch.match.fieldmatchers.IntTypeMatcher;
import org.forwoods.messagematch.match.fieldmatchers.NumTypeMatcher;

import java.util.*;

public class SchemaValidator {

    /**
     * LENIENT - we will ignore additional fields in the matcher method
     * STRICT - (Default) Extra fields are considered errors
     *
     * The default is STRICT - no-one can rely on unspecified values being present so their presence is a potential issue
     * more often than not this is a genuine break - it could be a simple as a typo in a field name or a more complicated misunderstanding of the contract
     * it is allowable for a message producer to include extra redundant fields - in which case use LENIENT but with less protection
     */
    public enum MatcherMode {
        LENIENT,
        STRICT
    }

    private final Schema<?> topSchema;
    private final MatcherMode mode;
    
    private final List<MatchError> validationErrors;

    public SchemaValidator(Schema<?> schema) {
        this(schema, MatcherMode.STRICT);
    }

    public SchemaValidator(Schema<?> schema, MatcherMode mode) {
        this.topSchema = schema;
        this.mode = mode;
        validationErrors = new ArrayList<>();
    }

    public boolean validate(JsonNode matcherNode) {
        return validate(new JsonPath("root", null), topSchema, matcherNode);
    }
    public boolean validate(JsonPath path, Schema<?> schema, JsonNode matcherNode) {
        switch (schema.getType()) {
            case "string":
                StringSchema strSc = (StringSchema) schema;
                if (!(matcherNode instanceof ValueNode)){
                    validationErrors.add(new MatchError(path, "Schema string","matcher "+matcherNode.getNodeType()));
                    return false;
                }
                return validateString(strSc, (ValueNode) matcherNode);
            case "number":
                NumberSchema numSch = (NumberSchema) schema;
                if(!(matcherNode instanceof ValueNode)) {
                    validationErrors.add(new MatchError(path, "Schema number","matcher "+matcherNode.getNodeType()));
                    return false;
                }
                return validateNumber(numSch, (ValueNode) matcherNode);
            case "integer":
                IntegerSchema intSch = (IntegerSchema) schema;
                if(!(matcherNode instanceof ValueNode)) {
                    validationErrors.add(new MatchError(path, "Schema number","matcher "+matcherNode.getNodeType()));
                    return false;
                }
                return validateInt(intSch, (ValueNode) matcherNode);
            case "boolean":
                BooleanSchema boolSch = (BooleanSchema) schema;
                if(!(matcherNode instanceof ValueNode)) {
                    validationErrors.add(new MatchError(path, "Schema number","matcher "+matcherNode.getNodeType()));
                    return false;
                }
                return validateBool(boolSch, (ValueNode) matcherNode);
            case "array":
                ArraySchema arr = (ArraySchema) schema;
                if (!(matcherNode instanceof ArrayNode)) {
                    validationErrors.add(new MatchError(path, "Schema array", "matcher"+matcherNode.getNodeType()));
                    return false;
                }
                return validateArray(path, arr, (ArrayNode)matcherNode);
            case "object":
                ObjectSchema obSc = (ObjectSchema) schema;
                if (!(matcherNode instanceof ObjectNode)) {
                    validationErrors.add(new MatchError(path, "Schema object", "matcher "+matcherNode.getNodeType()));
                    return false;
                }
                return validateObject(path, obSc, (ObjectNode) matcherNode);
        }
        validationErrors.add(new MatchError(path, "an implementation","validation for schema type "+schema.getType() + " not yet implemented"));
        return false;
    }

    private boolean validateBool(BooleanSchema boolSch, ValueNode matcherNode) {
        return matcherNode.isBoolean() || matcherNode.toString().equals("true") || matcherNode.toString().equals("false");
        //TODO other schema constraints ?
    }

    private boolean validateNumber(NumberSchema numSch, ValueNode matcherNode) {
        return matcherNode.isFloatingPointNumber() || NumTypeMatcher.isNumber(matcherNode.toString());
        //TODO other schema constraints
    }

    private boolean validateInt(IntegerSchema numSch, ValueNode matcherNode) {
        return matcherNode.canConvertToExactIntegral()|| IntTypeMatcher.isInt(matcherNode.toString());
        //TODO other schema constraints
    }

    private boolean validateString(StringSchema strSc, ValueNode matcherNode) {
        //the matcher being a primitive is probably good enough for now
        //TODO max length etc...
        return true;
    }

    private boolean validateObject(JsonPath path, ObjectSchema obSc, ObjectNode matcherNode) {
        Set<String> matchedProperties = new HashSet<>();
        boolean result = true;
        for (Iterator<Map.Entry<String, JsonNode>> it = matcherNode.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> field = it.next();
            String fieldName = field.getKey();
            Schema<?> fieldSchema = obSc.getProperties().get(fieldName);
            if (fieldSchema==null && mode==MatcherMode.STRICT) {
                validationErrors.add(new MatchError(path, "Schema node with name "+fieldName, "Not Found"));
                result = false;
            } else if (fieldSchema!=null){
                matchedProperties.add(fieldName);
                result &= validate(new JsonPath(fieldName, path), fieldSchema, field.getValue());
            }
        }
        if (obSc.getRequired()!=null) {
            for (String required : obSc.getRequired()) {
                if (!matchedProperties.contains(required)) {
                    validationErrors.add(new MatchError(path, "matcher containing " + required, "not present"));
                    result = false;
                }
            }
        }
        return result;
    }

    private boolean validateArray(JsonPath path, ArraySchema schema, ArrayNode matcherNode) {
        Schema<?> items = schema.getItems();
        boolean matches = true;
        for (int i=0;i<matcherNode.size();i++) {
           JsonNode jsonNode= matcherNode.get(i);
           matches&= validate(new JsonPath("[" + i + "]", path),items, jsonNode);
        }
        return matches;
    }

    public List<MatchError> getValidationErrors() {
        return validationErrors;
    }
}
