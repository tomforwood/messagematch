package org.forwoods.messagematch.messagematch.generate.nodegenerators;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class DateTypeGenerator extends NodeGenerator {

	JsonNode value;
	public DateTypeGenerator(String genVal, String binding, NodeGenerator today) {
		super(binding);
		if (genVal!=null) {
			value = parseDate(genVal);
		}
		else {
			value = today.generate();
		}
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
	protected JsonNode generate() {
		return value;
	}

}
