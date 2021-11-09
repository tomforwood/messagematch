package org.forwoods.messagematch.messagematch.generate;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.forwoods.messagematch.messagematch.generate.nodegenerators.NodeGenerator;
import org.forwoods.messagematch.messagematch.generate.nodegenerators.StringTypeGenerator;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherLexer;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;

public class JsonGenerator {
	static ObjectMapper mapper = new ObjectMapper();

	private InputStream matcherFile;

	private Map<String, NodeGenerator> bindings = new HashMap<>();
	
	protected long genTime=-1;//can be overridden for unit tests of the generator

	public JsonGenerator(InputStream matcherFile) {
		this.matcherFile = matcherFile;

	}

	public JsonNode generate() throws IOException {
		JsonNode matcherNode = mapper.readTree(matcherFile);
		
		if (genTime<0) genTime = System.currentTimeMillis();
		Instant i = Instant.ofEpochMilli(genTime);
		ZonedDateTime t = ZonedDateTime.ofInstant(i, ZoneOffset.UTC);
		LocalDate d = t.toLocalDate();
		LocalTime lt = t.toLocalTime();
		bindings.put("Date", new StringTypeGenerator(d.toString(), null));
		bindings.put("Time", new StringTypeGenerator(lt.toString(), null));
		bindings.put("Instant", new StringTypeGenerator(i.toString(), null));
		
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
		case ARRAY:
			return generateArray((ArrayNode) matcherNode);
		case NULL:
			break;
		default:
			break;

		}

		return matcherNode;

	}

	private JsonNode generatePrimitive(ValueNode matcherNode) {
		String matcher = matcherNode.asText();
		if (matcher.startsWith("$")) {
			return parseMatcher(matcher).generate(bindings );
		}
		if (matcher.startsWith("\\$")) {
			//do basic but with substitution
		}
		return matcherNode;
	}
	
	private NodeGenerator parseMatcher(String matcher) {
		MatcherLexer l = new MatcherLexer(CharStreams.fromString(matcher));
		MatcherParser p = new MatcherParser(new CommonTokenStream(l));
		p.addErrorListener(new BaseErrorListener() {
	        @Override
	        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
	            throw new IllegalStateException("failed to parse at line " + line + " due to " + msg, e);
	        }
	    });
		GrammarListenerGenerator listener = new GrammarListenerGenerator(bindings);
		p.addParseListener(listener);
		p.matcher();
		return listener.result;
	}

	private JsonNode generateArray(ArrayNode matcherNode) {
		ArrayNode newNode = mapper.createArrayNode();
		for (JsonNode jsonNode : matcherNode) {
			newNode.add(generate(jsonNode));
		}
		return newNode;
	}

	private JsonNode generateObject(ObjectNode matcherNode) {
		ObjectNode newNode = mapper.createObjectNode();
		for (Iterator<Map.Entry<String, JsonNode>> iterator = matcherNode.fields(); iterator.hasNext();) {
			Map.Entry<String, JsonNode> child = iterator.next();
			String key = child.getKey();
			if (key.equals("$Strict")) continue;
			if (key.startsWith("$")) {
				NodeGenerator gen = parseMatcher(key);
				JsonNode generate = gen.generate(bindings);
				key = generate.asText();
			}
			newNode.set(key, generate(child.getValue()));
		}
		return newNode;
	}

}
