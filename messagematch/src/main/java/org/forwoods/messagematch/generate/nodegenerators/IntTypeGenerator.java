package org.forwoods.messagematch.generate.nodegenerators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.forwoods.messagematch.generate.nodegenerators.constraints.Constraint;

public class IntTypeGenerator extends NodeGenerator {

	public IntTypeGenerator(ValueProvider provider) {
		super(provider);
		provider.addConstraint(Constraint.intConstraint);
	}


	@Override
	public JsonNode generate() {
		return JsonNodeFactory.instance.numberNode(provider.asInt());
	}


}
