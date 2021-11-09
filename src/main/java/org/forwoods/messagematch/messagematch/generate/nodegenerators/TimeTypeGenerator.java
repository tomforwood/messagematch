package org.forwoods.messagematch.messagematch.generate.nodegenerators;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class TimeTypeGenerator extends NodeGenerator {

	JsonNode value;
	public TimeTypeGenerator(String genVal, String binding, NodeGenerator now) {
		super(binding);
		if (genVal!=null) {
			value = parseDate(genVal);
		}
		else {
			value = now.generate();
		}
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
	protected JsonNode generate() {
		return value;
	}

}
