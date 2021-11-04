package org.forwoods.messagematch.messagematch.generate;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class NodeGenerator {
	
	private final String binding;
	
	public NodeGenerator(String binding) {
		super();
		this.binding = binding;
	}
	
	public JsonNode generate(Map<String,NodeGenerator> bindings) {
		if (binding!=null) {
			NodeGenerator bound = bindings.get(binding);
			if (bound!=null) return bound.generate();
		}
		JsonNode result = generate();
		if (binding!=null) {
			bindings.put(binding, this);
		}
		return result;
	}
	
	
	protected abstract JsonNode generate();

}
