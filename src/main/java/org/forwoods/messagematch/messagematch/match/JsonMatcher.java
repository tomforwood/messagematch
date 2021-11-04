package org.forwoods.messagematch.messagematch.match;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherLexer;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

public class JsonMatcher {
	static ObjectMapper mapper =  new ObjectMapper();
	
	private List<MatchError> errors= new ArrayList<>();

	private JsonNode matcherNode;

	private JsonNode concreteNode;
	
	private Map<String, String> bindings = new HashMap<>();
	
	public JsonMatcher(InputStream matcher, InputStream concrete) throws IOException {
		matcherNode = mapper.readTree(matcher);
		concreteNode = mapper.readTree(concrete);
	}

	public JsonMatcher(JsonNode node, JsonNode expected) {
		matcherNode = node;
		concreteNode = expected;
	}

	public boolean matches() throws IOException {
		return matches(new JsonPath("root", null), matcherNode, concreteNode);
	}

	public boolean matches(JsonPath path, JsonNode matcherNode, JsonNode concreteNode) {

		switch (matcherNode.getNodeType()) {

		case STRING:
		case BOOLEAN:
		case NUMBER:
			return matchPrimitive(path, (ValueNode)matcherNode, (ValueNode)concreteNode);
			
		case ARRAY:
			break;
		case NULL:
			break;
		case OBJECT:
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
		return listener.result;
	}
	


	private boolean matchObject(JsonPath path, ObjectNode matcherNode, ObjectNode concreteNode) {
		boolean result = true;
		for (Iterator<Map.Entry<String, JsonNode>> iterator = matcherNode.fields(); iterator.hasNext();) {
			Map.Entry<String, JsonNode> child = iterator.next();
			JsonNode matchedNode = concreteNode.get(child.getKey());
			if (matchedNode==null) {
				return false;
				//TODO error
			}
			else {
				boolean b = matches(new JsonPath(child.getKey(), path), child.getValue(), matchedNode);
				result = result & b;
			}
		}
		return result;
		
	}

	public List<MatchError> getErrors() {
		return errors;
	}
}
