package org.forwoods.messagematch.messagematch.generate.nodegenerators;

import java.time.Instant;
import java.time.format.DateTimeParseException;

import org.forwoods.messagematch.messagematch.match.fieldmatchers.InstantTypeMatcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class InstantTypeGenerator extends NodeGenerator {

	JsonNode value;
	public InstantTypeGenerator(String genVal, String binding, NodeGenerator now) {
		super(binding);
		if (genVal!=null) {
			value = parseInstant(genVal);
		}
		else {
			value = now.generate();
		}
		
	}

	@Override
	protected JsonNode generate() {
		return value;
	}
	
	private JsonNode parseInstant(String inst) {
		long ms;
		try {
			//try to read the supplied value as a ISO instant
			Instant i=InstantTypeMatcher.parses.apply(inst);
			return JsonNodeFactory.instance.textNode(i.toString());
		} catch (DateTimeParseException e) {}
		//try to read it as a long ms
		try {
			ms = Long.parseLong(inst);
			return JsonNodeFactory.instance.numberNode(ms);
		} catch (NumberFormatException e) {
		}
		throw new IllegalArgumentException("Provided value for instant of " + inst+ " could not be parsed");
	}

}
