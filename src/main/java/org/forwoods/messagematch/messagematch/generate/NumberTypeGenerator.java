package org.forwoods.messagematch.messagematch.generate;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class NumberTypeGenerator implements NodeGenerator {

	private final BigDecimal value;

	public NumberTypeGenerator(String value) {
		super();
		if (value == null) {
			this.value = BigDecimal.ZERO;
		} else {
			this.value = new BigDecimal(value);
		}
	}

	public NumberTypeGenerator(BigDecimal val) {
		value = val;		
	}

	@Override
	public JsonNode generate() {
		return JsonNodeFactory.instance.numberNode(value);
	}

}
