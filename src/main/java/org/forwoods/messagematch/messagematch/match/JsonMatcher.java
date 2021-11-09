package org.forwoods.messagematch.messagematch.match;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.forwoods.messagematch.messagematch.match.fieldmatchers.FieldMatcher;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherLexer;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

public class JsonMatcher {
	static ObjectMapper mapper =  new ObjectMapper();
	
	private List<MatchError> errors= new ArrayList<>();

	private JsonNode matcherNode;

	private JsonNode concreteNode;
	
	private Map<String, Object> bindings = new HashMap<>();
	
	protected long matchTime=-1;//can be overridden for unit tests of the matcher
	
	public JsonMatcher(InputStream matcher, InputStream concrete) throws IOException {
		this(mapper.readTree(matcher), mapper.readTree(concrete));
	}
	
	public JsonMatcher(JsonNode node, JsonNode expected) {
		matcherNode = node;
		concreteNode = expected;
	}

	public boolean matches() throws IOException {
		if (matchTime<0) matchTime = System.currentTimeMillis();
		Instant i = Instant.ofEpochMilli(matchTime);
		ZonedDateTime t = ZonedDateTime.ofInstant(i, ZoneOffset.UTC);
		LocalDate d = t.toLocalDate();
		LocalTime lt = t.toLocalTime();
		bindings.put("Date", d);
		bindings.put("Time", lt);
		bindings.put("Instant", i);
		
		return matches(new JsonPath("root", null), matcherNode, concreteNode);
	}

	public boolean matches(JsonPath path, JsonNode matcherNode, JsonNode concreteNode) {
		switch (matcherNode.getNodeType()) {

		case STRING:
		case BOOLEAN:
		case NUMBER:
			if (!(concreteNode instanceof ValueNode)) {
				errors.add(new MatchError(path, "a value node", "A structural node"));
			}
			return matchPrimitive(path, (ValueNode)matcherNode, (ValueNode)concreteNode);
			
		case ARRAY:
			if (!(concreteNode instanceof ArrayNode)) {
				errors.add(new MatchError(path, "a value node", "A structural node"));
			}
			return matchArray(path, (ArrayNode)matcherNode,(ArrayNode) concreteNode);
		case NULL:
			break;
		case OBJECT:
			if (!(concreteNode instanceof ObjectNode)) {
				errors.add(new MatchError(path, "a value node", "A structural node"));
			}
			return matchObject(path, (ObjectNode)matcherNode, (ObjectNode) concreteNode);
		case POJO:
		case MISSING:
		case BINARY:
			break;
		
		}
		
		return false;
		//TODO error
	}

	private boolean matchPrimitive(JsonPath path, ValueNode matcherNode, ValueNode concreteNode) {
		String matcher = matcherNode.asText();
		String concrete = concreteNode.asText();
		boolean matches = true;
		if (matcher.startsWith("$")) {
			try {
				FieldMatcher parseMatcher = parseMatcher(matcher);
				matches = parseMatcher.matches(concrete, bindings);
			}
			catch (UnboundVariableException e) {
				errors.add(new MatchError(path, e.getVar() +" to be bound", "unbound"));
				return false;
			}
		}
		else if (matcher.startsWith("\\$")) {
			matches = false;//TODO
		}
		else {
			matches = matcher.equals(concrete);
		}
		
		if (!matches) {
			errors.add(new MatchError(path, matcher, concrete));
		}
		return matches;
	}
	
	private FieldMatcher parseMatcher(String matcher) {
		MatcherLexer l = new MatcherLexer(CharStreams.fromString(matcher));
		MatcherParser p = new MatcherParser(new CommonTokenStream(l));
		p.addErrorListener(new BaseErrorListener() {
	        @Override
	        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
	            throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
	        }
	    });
		GrammarListenerMatcher listener = new GrammarListenerMatcher(bindings);
		p.addParseListener(listener);
		p.matcher();
		FieldMatcher result = listener.result;
		if (result==null) {
			throw new UnsupportedOperationException("cant parse matcher "+matcher);
		}
		return result;
	}

	private boolean matchArray(JsonPath path, ArrayNode matcherNode, ArrayNode concreteNode) {
		int matcherSize = matcherNode.size();
		int concreteSize = concreteNode.size();
		if (concreteSize < matcherSize) {
			errors.add(new MatchError(path, "an array of size "+matcherSize, Integer.toString(concreteSize)));
			return false;
		}
		boolean matches= true;
		for (int i=0;i<matcherSize;i++) {
			JsonNode matcherChild = matcherNode.get(i);
			JsonNode concreteChild = concreteNode.get(i);
			matches &= matches(new JsonPath("["+i+"]", path), matcherChild, concreteChild);
		}
		return matches;
	}

	private boolean matchObject(JsonPath path, ObjectNode matcherNode, ObjectNode concreteNode) {
		boolean result = true;
		boolean strictMode = false;
		List<String> matchedKeys = new ArrayList<>();
		for (Iterator<Map.Entry<String, JsonNode>> iterator = matcherNode.fields(); iterator.hasNext();) {
			Map.Entry<String, JsonNode> child = iterator.next();
			String key = child.getKey();
			
			Map<String,JsonNode> matchedNodes;
			if (key.startsWith("$")) {
				if (key.equals("$Strict")) {
					strictMode = true;
					continue;//ignore this
				}
				//interpret this node as a matcher
				FieldMatcher matcher = parseMatcher(key);
				matchedNodes = new LinkedHashMap<>();
				for (Iterator<Map.Entry<String, JsonNode>> citerator = concreteNode.fields(); citerator.hasNext();) {
					Map.Entry<String, JsonNode> cchild = citerator.next();
					if (matcher.matches(cchild.getKey(), bindings)) {
						matchedNodes.put(cchild.getKey(), cchild.getValue());
					}
				}
			}
			else {
				JsonNode matchedNode = concreteNode.get(key);
				if (matchedNode!=null) {
					matchedNodes = Map.of(key, matchedNode);
				}
				else {
					matchedNodes = Map.of();
				}
			}
			
			if (matchedNodes.isEmpty()) {
				errors.add(new MatchError(path, key, "not present"));
				return false;
			}
			else {
				matchedKeys.add(key);
				result = matchedNodes.entrySet().stream()
					.map((Function<Entry<String, JsonNode>, Boolean>) e->matches(new JsonPath(e.getKey(), path), 
							child.getValue(), 
							e.getValue())).reduce(result, (r,b)->r&b);
			}
		}
		if (strictMode) {
			List<String> concreteKeys = new ArrayList<>();
			concreteNode.fieldNames().forEachRemaining(s->concreteKeys.add(s));
			concreteKeys.removeAll(matchedKeys);
			if (!concreteKeys.isEmpty()) {
				errors.add(new MatchError(path, "no additional values", concreteKeys.toString()));
				result=false;
			}
		}
		
		return result;
		
	}

	public List<MatchError> getErrors() {
		return errors;
	}
}
