package org.forwoods.messagematch.generate.nodegenerators;

import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
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
