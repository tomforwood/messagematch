package org.forwoods.messagematch.generate.nodegenerators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.forwoods.messagematch.generate.nodegenerators.constraints.Constraint;

public class StringTypeGenerator extends NodeGenerator {


	public StringTypeGenerator(ValueProvider provider) {
		super(provider);
		provider.addConstraint(Constraint.stringConstraint);
	}

	@Override
	public JsonNode generate() {
		return JsonNodeFactory.instance.textNode(provider.asString());
	}

}
