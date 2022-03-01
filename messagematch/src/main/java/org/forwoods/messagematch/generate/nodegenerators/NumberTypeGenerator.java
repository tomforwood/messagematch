package org.forwoods.messagematch.generate.nodegenerators;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.forwoods.messagematch.generate.nodegenerators.constraints.Constraint;

public class NumberTypeGenerator extends NodeGenerator {

	public NumberTypeGenerator(ValueProvider provider) {
		super(provider);
		provider.addConstraint(Constraint.numberConstraint);
	}

	@Override
	public JsonNode generate() {
		return JsonNodeFactory.instance.numberNode(provider.asNum());
	}

}
