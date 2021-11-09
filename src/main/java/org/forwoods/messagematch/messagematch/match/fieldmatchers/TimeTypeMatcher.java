package org.forwoods.messagematch.messagematch.match.fieldmatchers;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class TimeTypeMatcher extends FieldMatcher {

	public TimeTypeMatcher(String binding) {
		super(binding);
		// TODO Auto-generated constructor stub
	}

	@Override
	boolean doMatch(String value) {
		try {
			return LocalTime.parse(value)!=null;
		} catch (DateTimeParseException e) {
			return false;
		}
	}

}
