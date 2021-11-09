package org.forwoods.messagematch.messagematch.match.fieldmatchers;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.function.Function;

public class InstantTypeMatcher extends FieldMatcher {

	public static Function<String, Instant> parses = s->Instant.parse(s);
	//public so it can be overridden for custom date parsers
	
	public InstantTypeMatcher(String binding) {
		super(binding);
	}

	@Override
	boolean doMatch(String value) {
		try {
			return parses.apply(value)!=null;
		} catch (DateTimeParseException e) {
			return false;
		}
	}

}
