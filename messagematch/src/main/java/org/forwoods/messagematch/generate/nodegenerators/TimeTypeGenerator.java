package org.forwoods.messagematch.generate.nodegenerators;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.forwoods.messagematch.generate.nodegenerators.constraints.Constraint;

public class TimeTypeGenerator extends NodeGenerator {

	public TimeTypeGenerator(ValueProvider provider, String time) {
		super(provider);
		provider.addConstraint(Constraint.timeConstraint(time));
	}

	private JsonNode parseDate(String genVal) {
		try {
			//try to read the supplied value as a ISO instant
			LocalTime i=LocalTime.parse(genVal);
			return JsonNodeFactory.instance.textNode(i.toString());
		} catch (DateTimeParseException e) {}
		throw new IllegalArgumentException("Provided value for localtime of " + genVal+ " could not be parsed");
	}

	@Override
	public JsonNode generate() {
		return JsonNodeFactory.instance.textNode(provider.asTime());
	}

}
