package org.forwoods.messagematch.generate.nodegenerators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.forwoods.messagematch.generate.nodegenerators.constraints.Constraint;

public class BoolTypeGenerator extends NodeGenerator {

	public BoolTypeGenerator(ValueProvider provider) {
		super(provider);
		provider.addConstraint(Constraint.boolConstraint);
	}


	@Override
	public JsonNode generate() {
		return JsonNodeFactory.instance.booleanNode(provider.asBoolean());
	}


}
