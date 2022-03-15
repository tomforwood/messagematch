package org.forwoods.messagematch.match.fieldmatchers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public class DateTypeMatcher extends FieldMatcher<ChronoLocalDate> {

	public DateTypeMatcher(String binding, boolean nullable, FieldComparatorMatcher comparator) {
		super(binding, nullable, comparator);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected ChronoLocalDate asComparable(String val) {
		return LocalDate.parse(val);
	}

	@Override
	boolean doMatch(String value) {
		try {
			return LocalDate.parse(value)!=null;
		} catch (DateTimeParseException e) {
			return false;
		}
	}

	@Override
	public boolean doSymRange(ChronoLocalDate value, ChronoLocalDate compareTo, String s) {
		ChronoLocalDate start;
		ChronoLocalDate end;
		try {
			Duration d = Duration.parse(s);
			if (d.isNegative()) {
				end = compareTo.minus(d);
				start = compareTo.plus(d);
			}
			else {
				start = compareTo.minus(d);
				end = compareTo.plus(d);
			}
		}
		catch (DateTimeParseException e) {
			long seconds = Long.parseLong(s);
			if (seconds<0) {
				start = compareTo.plus(seconds, ChronoUnit.DAYS);
				end = compareTo.minus(seconds, ChronoUnit.DAYS);
			}
			else {
				end = compareTo.plus(seconds, ChronoUnit.DAYS);
				start = compareTo.minus(seconds, ChronoUnit.DAYS);
			}
		}
		return !value.isAfter(end) && !value.isBefore(start);
	}

	@Override
	public boolean doASymRange(ChronoLocalDate value, ChronoLocalDate compareTo, String s) {
		ChronoLocalDate start;
		ChronoLocalDate end;
		try {
			Duration d = Duration.parse(s);
			if (d.isNegative()) {
				end = compareTo;
				start = compareTo.plus(d);
			}
			else {
				start = compareTo;
				end = compareTo.plus(d);
			}
		}
		catch (DateTimeParseException e) {
			long seconds = Long.parseLong(s);
			if (seconds<0) {
				start = compareTo.plus(seconds, ChronoUnit.DAYS);
				end = compareTo;
			}
			else {
				end = compareTo.plus(seconds, ChronoUnit.DAYS);
				start = compareTo;
			}
		}
		return !value.isAfter(end) && !value.isBefore(start);
	}

}
