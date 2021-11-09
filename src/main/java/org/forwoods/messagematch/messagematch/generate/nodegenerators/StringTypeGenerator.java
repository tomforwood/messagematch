package org.forwoods.messagematch.messagematch.generate.nodegenerators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class StringTypeGenerator extends NodeGenerator {

	String value;
	public StringTypeGenerator(String value,String binding) {
		super(binding);
		if (value == null) {
			this.value = "";
		} else {
			this.value = value;
		}
	}

	@Override
	protected JsonNode generate() {
		return JsonNodeFactory.instance.textNode(value);
	}

}
