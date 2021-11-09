package org.forwoods.messagematch.messagematch.match.fieldmatchers;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public class DateTypeMatcher extends FieldMatcher {

	public DateTypeMatcher(String binding) {
		super(binding);
		// TODO Auto-generated constructor stub
	}

	@Override
	boolean doMatch(String value) {
		try {
			return LocalDate.parse(value)!=null;
		} catch (DateTimeParseException e) {
			return false;
		}
	}

}
