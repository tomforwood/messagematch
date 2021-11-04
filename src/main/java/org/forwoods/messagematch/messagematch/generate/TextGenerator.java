package org.forwoods.messagematch.messagematch.generate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class TextGenerator extends NodeGenerator {

	private String text;

	public TextGenerator(String text, String binding) {
		super(binding);
		this.text = text;
	}

	@Override
	public JsonNode generate() {
		return JsonNodeFactory.instance.textNode(text);
	}

}
