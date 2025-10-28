package org.forwoods.messagematch.generate;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.forwoods.messagematch.generate.nodegenerators.*;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.forwoods.messagematch.generate.nodegenerators.constraints.ProvidedConstraint;
import org.forwoods.messagematch.matchgrammar.MatcherLexer;
import org.forwoods.messagematch.matchgrammar.MatcherParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import org.forwoods.messagematch.util.PathExtractor;

import static org.forwoods.messagematch.match.JsonMatcher.sizePattern;

/**
 * @author Tom
 *
 */
public class JsonGenerator {
	static final ObjectMapper mapper = new ObjectMapper();

	private final JsonNode matcherNode;

	private final Map<String, ValueProvider> bindings;

	private final Map<String, Object> rawBindings;
	
	protected long genTime=-1;//can be overridden for unit tests of the generator

	public JsonGenerator(InputStream matcherFile, Map<String, Object> bindings) throws IOException {
		this(mapper.readTree(matcherFile), bindings);
	}
	public JsonGenerator(InputStream matcherFile) throws IOException {
		this(mapper.readTree(matcherFile), Map.of());
	}

	public JsonGenerator(String matcherString, Map<String, Object> bindings) throws JsonProcessingException {
		this(mapper.readTree(matcherString), bindings);
	}
	public JsonGenerator(String matcherString) throws IOException {
		this(mapper.readTree(matcherString), Map.of());
	}
	public JsonGenerator(JsonNode matcherNode) {
		this(matcherNode, Map.of());
	}
	public JsonGenerator(JsonNode matcherNode, Map<String, Object> bindings) {
		this.matcherNode = matcherNode;
		this.bindings=bindings.entrySet().stream()
				.collect(
						Collectors.toMap(Map.Entry::getKey, e-> createValueProvider(e.getValue().toString())));
		this.rawBindings = bindings;
	}

	public static ValueProvider createValueProvider(String val) {
		ValueProvider res = new ValueProvider();
		res.addConstraint(new ProvidedConstraint(val));
		return res;
	}

	public JsonNode generate() {

		if (genTime<0) genTime = System.currentTimeMillis();
		Instant i = Instant.ofEpochMilli(genTime);
		ZonedDateTime t = ZonedDateTime.ofInstant(i, ZoneOffset.UTC);
		LocalDate d = t.toLocalDate();
		LocalTime localTime = t.toLocalTime();
		bindings.put("date", createValueProvider(d.toString()));
		bindings.put("time", createValueProvider(localTime.toString()));
		bindings.put("datetime", createValueProvider(Long.toString(genTime)));
		
		return generate(matcherNode).generate();
	}

	public String generateString() {
		return generate().toPrettyString();
	}

	public NodeGenerator generate(JsonNode matcherNode) {
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

		return new LiteralGenerator(matcherNode);

	}

	private NodeGenerator generatePrimitive(ValueNode matcherNode) {
		String matcher = matcherNode.asText();
		if (matcher.startsWith("$ID")) {
			JsonNode bound = PathExtractor.extractPrimitiveNode(matcher,rawBindings);
			return new ReferenceGenerator(bound);
		}
		if (matcher.startsWith("$")) {
			return parseMatcher(matcher, bindings);
		}
		if (matcher.startsWith("\\$")) {
			return new LiteralGenerator(JsonNodeFactory.instance.textNode(matcher.replaceFirst("\\$", "$")));
		}
		return new LiteralGenerator(matcherNode);
	}
	
	public static NodeGenerator parseMatcher(String matcher, Map<String, ValueProvider> bindings) {
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

	private NodeGenerator generateArray(ArrayNode matcherNode) {
		ArrayTypeGenerator arrayGen = new ArrayTypeGenerator();
		ArraySizeGenerator sizeGen = null;
		for (JsonNode jsonNode : matcherNode) {
			NodeGenerator generate = generate(jsonNode);
			if (generate instanceof ArraySizeGenerator) {
				sizeGen = (ArraySizeGenerator) generate;
				continue;
			}
			arrayGen.addChild(generate);
		}

		if (sizeGen!=null) {
			arrayGen.setSize(sizeGen.getMin(), sizeGen.getMax());
		}

		return arrayGen;
	}

	private NodeGenerator generateObject(ObjectNode matcherNode) {
		ObjectTypeGenerator object = new ObjectTypeGenerator();
		for (Iterator<Map.Entry<String, JsonNode>> iterator = matcherNode.fields(); iterator.hasNext();) {
			Map.Entry<String, JsonNode> child = iterator.next();
			String key = child.getKey();
			if (key.equals("$Strict")) continue;
			if (key.equals("$Size")) {
				JsonNode node = child.getValue();
				String bounds = node.asText();
				Matcher m = sizePattern.matcher(bounds);
				Integer min = null;
				Integer max = null;
				if (m.matches()) {
					if (m.group(1).length() > 0) {
						min = Integer.valueOf(m.group(1));
					}
					if (m.group(2).length() > 0) {
						max = Integer.valueOf(m.group(2));
					}
				}
				return new ArraySizeGenerator(min, max);}
			if (key.equals("$ID")) {
				JsonNode bound = PathExtractor.extractObjectNode(child.getValue().asText(),rawBindings);
				if (bound!=null)
					return new ReferenceGenerator(bound);
				continue;
			}
			if (key.startsWith("$")) {
				NodeGenerator gen = parseMatcher(key, bindings);
				JsonNode generate = gen.generate();
				key = generate.asText();
			}
			object.addChild(key, generate(child.getValue()));
		}
		return object;
	}

}
