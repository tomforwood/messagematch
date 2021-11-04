package org.forwoods.messagematch.messagematch.generate;

import java.math.BigInteger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class IntTypeGenerator implements NodeGenerator {

	private final BigInteger value;

	public IntTypeGenerator(String value) {
		super();
		if (value == null) {
			this.value = BigInteger.ZERO;
		} else {
			this.value = new BigInteger(value);
		}
	}

	@Override
	public JsonNode generate() {
		return JsonNodeFactory.instance.numberNode(value);
	}

}
