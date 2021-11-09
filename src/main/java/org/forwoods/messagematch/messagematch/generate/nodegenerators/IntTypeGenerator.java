package org.forwoods.messagematch.messagematch.generate.nodegenerators;

import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

public class IntTypeGenerator extends NodeGenerator {

	private final BigInteger value;

	public IntTypeGenerator(String value, String binding) {
		super(binding);
		if (value == null) {
			this.value = BigInteger.ZERO;
		} else {
			this.value = new BigInteger(value);
		}
	}

	@Override
	public JsonNode generate(Map<String, NodeGenerator> bindings) {
		JsonNode node = super.generate(bindings);
		if (node instanceof TextNode) {
			//try to parse the test and transform to number node
			try {
				BigInteger bi = new BigInteger(node.textValue());
				return JsonNodeFactory.instance.numberNode(bi);
			}
			catch (NumberFormatException e) {}
			try {
				Instant i = Instant.parse(node.textValue());
				return JsonNodeFactory.instance.numberNode(i.toEpochMilli());
			}
			catch (DateTimeParseException e) {}
			throw new IllegalArgumentException("Bound to value that is not int type");
		}
		else return node;
	}

	@Override
	public JsonNode generate() {
		return JsonNodeFactory.instance.numberNode(value);
	}

}
