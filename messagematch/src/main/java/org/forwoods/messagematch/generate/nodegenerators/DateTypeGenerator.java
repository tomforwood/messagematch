package org.forwoods.messagematch.generate.nodegenerators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.forwoods.messagematch.generate.nodegenerators.constraints.Constraint;

public class DateTypeGenerator extends NodeGenerator {

	public DateTypeGenerator(ValueProvider provider, String date) {
		super(provider);
		provider.addConstraint(Constraint.dateConstraint(date));
	}

	@Override
	public JsonNode generate() {
		return JsonNodeFactory.instance.textNode(provider.asDate());
	}

}
