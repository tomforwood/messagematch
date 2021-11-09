package org.forwoods.messagematch.messagematch.generate.nodegenerators;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class NumTypeGenerator extends NodeGenerator {

	private final BigDecimal value;

	public NumTypeGenerator(String value, String binding) {
		super(binding);
		if (value == null) {
			this.value = BigDecimal.ZERO;
		} else {
			this.value = new BigDecimal(value);
		}
	}

	@Override
	public JsonNode generate() {
		return JsonNodeFactory.instance.numberNode(value);
	}

}
