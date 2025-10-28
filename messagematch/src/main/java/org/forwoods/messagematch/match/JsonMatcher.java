package org.forwoods.messagematch.match;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.NullNode;
import org.forwoods.messagematch.match.fieldmatchers.FieldMatcher;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.forwoods.messagematch.matchgrammar.MatcherLexer;
import org.forwoods.messagematch.matchgrammar.MatcherParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import org.forwoods.messagematch.util.PathExtractor;

import static com.fasterxml.jackson.databind.node.JsonNodeType.MISSING;
import static com.fasterxml.jackson.databind.node.JsonNodeType.NULL;

/**
 * @author Tom
 *
 */
public class JsonMatcher {
    static final ObjectMapper mapper = new ObjectMapper();

    private List<MatchError> errors = new ArrayList<>();

    private final JsonNode matcherNode;

    private final JsonNode concreteNode;

    private final Map<String, Object> bindings = new HashMap<>();

    protected long matchTime = -1;//can be overridden for unit tests of the matcher

    public JsonMatcher(InputStream matcher, InputStream concrete) throws IOException {
        this(readNodes(matcher), mapper.readTree(concrete));
    }

    public static JsonNode readNodes(InputStream matcher) throws IOException {
        return mapper.readTree(matcher);
    }

    public JsonMatcher(String matcher, String concrete) throws IOException {
        this(readNodes(matcher), readNodes(concrete));
    }

    public static JsonNode readNodes(String matcher) throws JsonProcessingException {
        return mapper.readTree(matcher);
    }

    public JsonMatcher(JsonNode matcher, String concrete) throws JsonProcessingException {
        this(matcher, readNodes(concrete));
    }

    public JsonMatcher(JsonNode matcher, JsonNode actual) {
        matcherNode = matcher;
        concreteNode = actual;
    }

    public enum MatcherOption{
        UNORDERED,
        STRICT
    }

    final Set<MatcherOption> options = new HashSet<>();

    boolean hasOption(MatcherOption option) {
        return options.contains(option);
    }

    public boolean matches(MatcherOption... options){
        Collections.addAll(this.options, options);
        return matches();
    }

    public boolean matches() {
        if (matchTime < 0) matchTime = System.currentTimeMillis();
        Instant i = Instant.ofEpochMilli(matchTime);
        ZonedDateTime t = ZonedDateTime.ofInstant(i, ZoneOffset.UTC);
        LocalDate d = t.toLocalDate();
        LocalTime lt = t.toLocalTime();
        bindings.put("date", d);
        bindings.put("time", lt);
        bindings.put("datetime", matchTime);

        return matches(new JsonPath("root", null), matcherNode, concreteNode);
    }

    private boolean matches(JsonPath path, JsonNode matcherNode, JsonNode concreteNode) {
        switch (matcherNode.getNodeType()) {

            case STRING:
            case BOOLEAN:
            case NUMBER:
                if (!(concreteNode instanceof ValueNode)) {
                    errors.add(new MatchError(path, "a value node", "A "+concreteNode.getNodeType() + " node"));
                    return false;
                }
                return matchPrimitive(path, (ValueNode) matcherNode, (ValueNode) concreteNode);

            case ARRAY:
                if (!(concreteNode instanceof ArrayNode)) {
                    errors.add(new MatchError(path, "an array node", "A "+concreteNode.getNodeType() + " node"));
                    return false;
                }
                return matchArray(path, (ArrayNode) matcherNode, (ArrayNode) concreteNode);
            case NULL:
                if (concreteNode.getNodeType()!=NULL) {
                    errors.add(new MatchError(path, "a null value", "a node with a value "+ concreteNode));
                    return false;
                }
                return true;
            case OBJECT:
                if (!(concreteNode instanceof ObjectNode)) {
                    errors.add(new MatchError(path, "an object node", "A "+concreteNode.getNodeType() + " node"));
                    return false;
                }
                return matchObject(path, (ObjectNode) matcherNode, (ObjectNode) concreteNode);
            case MISSING:
                if (!(concreteNode.getNodeType().equals(MISSING))) {
                    errors.add(new MatchError(path, "no node at all", "a node"));
                    return false;
                }
            case POJO:
            case BINARY:
                break;
        }
        errors.add(new MatchError(path, " a match implementation for " + matcherNode.getNodeType(), "Unimplemented"));
        return false;
    }

    private boolean matchPrimitive(JsonPath path, ValueNode matcherNode, ValueNode concreteNode) {
        String matcher = matcherNode.asText();
        String concrete = null;
        if (!(concreteNode instanceof NullNode)){
            concrete = concreteNode.asText();
        }
        boolean matches;
        if (matcher.startsWith("$")) {
            if (matcher.startsWith("$ID")) {
                JsonNode objRef = PathExtractor.extractPrimitiveNode(matcher,bindings);
                matches = matchPrimitive(path, (ValueNode)objRef, concreteNode);
            }
            else {
                try {
                    FieldMatcher<?> parseMatcher = parseMatcher(matcher);
                    matches = parseMatcher.matches(concrete, bindings);
                } catch (UnboundVariableException e) {
                    errors.add(new MatchError(path, e.getVar() + " to be bound", "unbound"));
                    return false;
                }
            }
        } else if (matcher.startsWith("\\$")) {
            String test = matcher.substring(1);
            return test.equals(concrete);
        } else {
            matches = matcher.equals(concrete);
        }

        if (!matches) {
            errors.add(new MatchError(path, matcher, concrete));
        }
        return matches;
    }

    public static FieldMatcher<?> parseMatcher(String matcher) {
        MatcherLexer l = new MatcherLexer(CharStreams.fromString(matcher));
        MatcherParser p = new MatcherParser(new CommonTokenStream(l));
        p.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
            }
        });
        GrammarListenerMatcher listener = new GrammarListenerMatcher();
        p.addParseListener(listener);
        p.matcher();
        FieldMatcher<?> result = listener.result;
        if (result == null) {
            throw new UnsupportedOperationException("cant parse matcher " + matcher);
        }
        return result;
    }

    public final static Pattern sizePattern = Pattern.compile("([0-9]*)-([0-9]*)");

    private boolean matchArray(JsonPath path, ArrayNode matcherNode, ArrayNode concreteNode) {
        int matcherSize = matcherNode.size();
        int concreteSize = concreteNode.size();

        //special flags
        boolean strict = hasOption(MatcherOption.STRICT);
        boolean unordered = hasOption(MatcherOption.UNORDERED);
        boolean each = false;
        int min = 0;
        int max = Integer.MAX_VALUE;
        int offset = 0;
        boolean hasSize=false;

        if (matcherSize == 0) {
            if (concreteSize == 0) return true;
            else {
                errors.add(new MatchError(path, "and empty array", "size " + concreteSize));
            }
        }
        //look for the special matching node
        JsonNode n = matcherNode.get(0);
        if (n instanceof ObjectNode) {
            ObjectNode on = (ObjectNode) n;
            if (on.size() > 0) {
                if (on.fieldNames().next().startsWith("$")) {
                    //this is a "special" object
                    strict = on.has("$Strict");
                    unordered = on.has("$Unordered");
                    each = on.has("$Each");
                    JsonNode sizeNode = on.get("$Size");
                    if (sizeNode != null) {
                        hasSize = true;
                        String bounds = sizeNode.asText();
                        Matcher m = sizePattern.matcher(bounds);
                        if (m.matches()) {
                            if (m.group(1).length() > 0) {
                                min = Integer.parseInt(m.group(1));
                            }
                            if (m.group(2).length() > 0) {
                                max = Integer.parseInt(m.group(2));
                            }
                        } else {
                            errors.add(new MatchError(path, "size shoud be \"min-max\"", bounds));
                            return false;
                        }
                    }
                    if (strict || unordered || each || hasSize) {
                        offset = 1;
                        matcherSize--;
                    }
                }
            }
        }

        boolean matches = true;

        if (concreteSize < matcherSize) {
            errors.add(new MatchError(path, "an array of at least size " + matcherSize, Integer.toString(concreteSize)));
            return false;
        }

        if (strict && concreteSize > matcherSize) {
            errors.add(new MatchError(path, "an array of at exactly size " + matcherSize, Integer.toString(concreteSize)));
            matches = false;
        }

        if (concreteSize < min) {
            errors.add(new MatchError(path, "an array of at least size " + min, Integer.toString(concreteSize)));
            matches=false;
        }
        if (concreteSize > max) {
            errors.add(new MatchError(path, "an array of at most size " + max, Integer.toString(concreteSize)));
            matches = false;
        }

        if (!unordered) {
            for (int i = 0; i < matcherSize - offset; i++) {
                JsonNode matcherChild;
                if (each) {
                    matcherChild = matcherNode.get(offset);
                } else {
                    matcherChild = matcherNode.get(i + offset);
                }
                JsonNode concreteChild = concreteNode.get(i);
                matches &= matches(new JsonPath("[" + i + "]", path), matcherChild, concreteChild);
            }
        }
        else {
            List<JsonNode> leftToMatch = new ArrayList<>();
            concreteNode.elements().forEachRemaining(leftToMatch::add);
            //The test matchings here are going to make a mess of our errors list so lets temporarily replace it
            List<MatchError> realErrors = errors;
            errors = new ArrayList<>();
            for (int i = 0; i < matcherSize - offset; i++) {
                JsonNode matcherChild;
                if (each) {
                    matcherChild = matcherNode.get(offset);
                } else {
                    matcherChild = matcherNode.get(i + offset);
                }
                boolean isMatched=false;
                for (int j=0;j<leftToMatch.size();j++) {
                    isMatched = matches(new JsonPath("[" + i + "]", path), matcherChild, leftToMatch.get(j));
                    if (isMatched) {
                        leftToMatch.remove(j);
                        break;
                    }
                }
                if (!isMatched) {
                    realErrors.add(new MatchError(path, "an object matching "+matcherChild,"nothing matching"));
                }
                matches &= isMatched;
            }
            errors = realErrors;
        }
        return matches;
    }

    private boolean matchObject(JsonPath path, ObjectNode matcherNode, ObjectNode concreteNode) {
        boolean result = true;
        boolean strictMode = false;
        List<String> matchedKeys = new ArrayList<>();
        String id = null;
        for (Iterator<Map.Entry<String, JsonNode>> iterator = matcherNode.fields(); iterator.hasNext(); ) {
            Map.Entry<String, JsonNode> child = iterator.next();
            String key = child.getKey();

            Map<String, JsonNode> matchedNodes;
            if (key.startsWith("$")) {
                if (key.equals("$Strict")) {
                    strictMode = true;
                    continue;//ignore this
                }
                if (key.equals("$Size")) {
                    String bounds = child.getValue().asText();
                    Matcher m = sizePattern.matcher(bounds);
                    if (m.matches()) {
                        if (m.group(1).length() > 0) {
                            int min = Integer.parseInt(m.group(1));
                            if (concreteNode.size() > min) {
                                errors.add(new MatchError(path, "at least " + min + " keys", Integer.toString(concreteNode.size())));
                            }
                        }
                        if (m.group(2).length() > 0) {
                            int max = Integer.parseInt(m.group(2));
                            if (concreteNode.size() < max) {
                                errors.add(new MatchError(path, "at most " + max + " keys", Integer.toString(concreteNode.size())));
                            }
                        }
                    } else {
                        errors.add(new MatchError(path, "size shoud be \"min-max\"", bounds));
                        return false;
                    }
                }
                if (key.equals("$ID")) {
                    id = child.getValue().asText();
                    continue;
                }
                //interpret this node as a matcher
                FieldMatcher<?> matcher = parseMatcher(key);
                matchedNodes = new LinkedHashMap<>();
                for (Iterator<Entry<String, JsonNode>> citerator = concreteNode.fields(); citerator.hasNext(); ) {
                    Entry<String, JsonNode> cchild = citerator.next();
                    if (matcher.matches(cchild.getKey(), bindings)) {
                        matchedNodes.put(cchild.getKey(), cchild.getValue());
                    }
                }
            } else if (key.startsWith("\\$")) {
                key = key.substring(1);
                JsonNode matchedNode = concreteNode.get(key);
                if (matchedNode != null) {
                    matchedNodes = Map.of(key, matchedNode);
                } else {
                    matchedNodes = Map.of();
                }
            } else {
                JsonNode matchedNode = concreteNode.get(key);
                if (matchedNode != null) {
                    matchedNodes = Map.of(key, matchedNode);
                } else {
                    matchedNodes = Map.of();
                }
            }

            if (matchedNodes.isEmpty()) {
                errors.add(new MatchError(path, key, "not present"));
                return false;
            } else {
                matchedKeys.add(key);
                result &= matchedNodes.entrySet().stream()
                        .map(e -> matches(new JsonPath(e.getKey(), path),
                                child.getValue(),
                                e.getValue())).reduce(result, (r, b) -> r & b);
            }
        }
        if (strictMode) {
            List<String> concreteKeys = new ArrayList<>();
            concreteNode.fieldNames().forEachRemaining(concreteKeys::add);
            concreteKeys.removeAll(matchedKeys);
            if (!concreteKeys.isEmpty()) {
                errors.add(new MatchError(path, "no additional values", concreteKeys.toString()));
                result = false;
            }
        }

        if (result && id!=null) {
            bindings.put(id, concreteNode);
        }

        return result;

    }

    public List<MatchError> getErrors() {
        return errors;
    }

    public Map<String, Object> getBindings() {
        return bindings;
    }
}
