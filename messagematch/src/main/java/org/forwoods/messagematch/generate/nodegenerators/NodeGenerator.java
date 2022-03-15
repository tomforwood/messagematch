package org.forwoods.messagematch.generate.nodegenerators;

import com.fasterxml.jackson.databind.JsonNode;

public abstract class NodeGenerator {

	protected final ValueProvider provider;

	protected NodeGenerator(ValueProvider provider) {
		this.provider = provider;
	}

	public abstract JsonNode generate();
}
