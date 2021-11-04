package org.forwoods.messagematch.messagematch.generate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
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

public class JsonGenerator {
	static ObjectMapper mapper = new ObjectMapper();

	private InputStream matcherFile;

	public JsonGenerator(InputStream matcherFile) {
		this.matcherFile = matcherFile;

	}

	public JsonNode generate() throws IOException {
		JsonNode matcherNode = mapper.readTree(matcherFile);
		return generate(matcherNode);
	}

	public JsonNode generate(JsonNode matcherNode) {
		switch (matcherNode.getNodeType()) {
		case STRING:
		case BOOLEAN:
		case NUMBER:
			return generatePrimitive((ValueNode) matcherNode);

		case OBJECT:
			return generateObject((ObjectNode) matcherNode);
		case POJO:
		case MISSING:
		case BINARY:
			break;

		}

		return matcherNode;

	}

	private JsonNode generatePrimitive(ValueNode matcherNode) {
		String matcher = matcherNode.asText();
		if (matcher.startsWith("$")) {
			return parseMatcher(matcher).generate();
		}
		if (matcher.startsWith("\\$")) {
			//do basic but with substitution
		}
		return matcherNode;
	}
	
	static NodeGenerator parseMatcher(String matcher) {
		MatcherLexer l = new MatcherLexer(CharStreams.fromString(matcher));
		MatcherParser p = new MatcherParser(new CommonTokenStream(l));
		p.addErrorListener(new BaseErrorListener() {
	        @Override
	        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
	            throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
	        }
	    });
		GrammarListenerGenerator listener = new GrammarListenerGenerator();
		p.addParseListener(listener);
		p.matcher();
		return listener.result;
	}

	private JsonNode generateObject(ObjectNode matcherNode) {
		ObjectNode newNode = mapper.createObjectNode();
		for (Iterator<Map.Entry<String, JsonNode>> iterator = matcherNode.fields(); iterator.hasNext();) {
			Map.Entry<String, JsonNode> child = iterator.next();
			newNode.set(child.getKey(), generate(child.getValue()));
		}
		return newNode;
	}

}
