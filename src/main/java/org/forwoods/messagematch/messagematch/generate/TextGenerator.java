package org.forwoods.messagematch.messagematch.generate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class TextGenerator implements NodeGenerator {

	private String text;

	public TextGenerator(String text) {
		this.text = text;
		// TODO Auto-generated constructor stub
	}

	@Override
	public JsonNode generate() {
		return JsonNodeFactory.instance.textNode(text);
	}

}
