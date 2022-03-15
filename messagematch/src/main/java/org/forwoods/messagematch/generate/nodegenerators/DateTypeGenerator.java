package org.forwoods.messagematch.generate.nodegenerators;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.forwoods.messagematch.generate.nodegenerators.constraints.Constraint;

public class DateTypeGenerator extends NodeGenerator {

	public DateTypeGenerator(ValueProvider provider, String date) {
		super(provider);
		provider.addConstraint(Constraint.dateConstraint(date));
	}

	private JsonNode parseDate(String genVal) {
		try {
			//try to read the supplied value as a ISO instant
			LocalDate i=LocalDate.parse(genVal);
			return JsonNodeFactory.instance.textNode(i.toString());
		} catch (DateTimeParseException e) {}
		throw new IllegalArgumentException("Provided value for localdate of " + genVal+ " could not be parsed");

	}

	@Override
	public JsonNode generate() {
		return JsonNodeFactory.instance.textNode(provider.asDate());
	}

}
