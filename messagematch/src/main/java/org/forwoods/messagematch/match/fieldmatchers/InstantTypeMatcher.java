package org.forwoods.messagematch.match.fieldmatchers;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.function.Function;

public class InstantTypeMatcher extends FieldMatcher<Instant> {

	public static Function<String, Instant> parses = s->Instant.parse(s);
	//public so it can be overridden for custom date parsers
	
	public InstantTypeMatcher(String binding, boolean nullable, FieldComparatorMatcher comparator) {
		super(binding, nullable, comparator);
	}

	@Override
	protected Instant asComparable(String val) {
		try {
			Instant result = parses.apply(val);
			return result;
		} catch (DateTimeParseException e) {
			long millis = Long.parseLong(val);
			return Instant.ofEpochMilli(millis);
		}
	}

	@Override
	boolean doMatch(String value) {
		try {
			return parses.apply(value)!=null;
		} catch (DateTimeParseException e) {
			return false;
		}
	}

	@Override
	public boolean doSymRange(Instant value, Instant compareTo, String s) {
		return TimeTypeMatcher.doSymRangeTemporal(value, compareTo, s);
	}

	@Override
	public boolean doASymRange(Instant value, Instant compareTo, String s) {
		return TimeTypeMatcher.doASymRangeTemporal(value, compareTo, s);
	}

}
