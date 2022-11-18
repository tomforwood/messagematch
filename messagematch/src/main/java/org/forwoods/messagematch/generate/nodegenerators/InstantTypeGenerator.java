package org.forwoods.messagematch.generate.nodegenerators;

import java.math.BigInteger;

import org.forwoods.messagematch.generate.nodegenerators.constraints.Constraint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class InstantTypeGenerator extends NodeGenerator {

	public InstantTypeGenerator(ValueProvider provider, BigInteger datetime) {
		super(provider);
		provider.addConstraint(Constraint.instantConstraint(datetime));
	}

	@Override
	public JsonNode generate() {
		return JsonNodeFactory.instance.textNode(provider.asInstant());
	}

}
