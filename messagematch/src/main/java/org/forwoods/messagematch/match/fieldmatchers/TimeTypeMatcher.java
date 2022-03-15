package org.forwoods.messagematch.match.fieldmatchers;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;

public class TimeTypeMatcher extends FieldMatcher<LocalTime> {

	public TimeTypeMatcher(String binding, boolean nullable, FieldComparatorMatcher comparator) {
		super(binding, nullable, comparator);
	}

	@Override
	protected LocalTime asComparable(String val) {
		return LocalTime.parse(val);
	}

	@Override
	boolean doMatch(String value) {
		try {
			return LocalTime.parse(value)!=null;
		} catch (DateTimeParseException e) {
			return false;
		}
	}

	@Override
	public boolean doSymRange(LocalTime value, LocalTime compareTo, String s) {
		return doSymRangeTemporal(value, compareTo, s);
	}

	@Override
	public boolean doASymRange(LocalTime value, LocalTime compareTo, String s) {
		return doASymRangeTemporal(value, compareTo, s);
	}

	private static final BigDecimal billion = BigDecimal.valueOf(1000000000);
	protected static boolean doSymRangeTemporal(Temporal value, Temporal compareTo, String s) {
		Duration actual = Duration.between(value, compareTo).abs();
		Duration range = getDuration(s).abs();
		return actual.compareTo(range) <=0;
	}

	private static Duration getDuration(String s) {
		Duration range;
		try {
			range = Duration.parse(s).abs();
		}
		catch (DateTimeParseException e) {
			BigDecimal seconds = new BigDecimal(s);
			range = Duration.of(seconds.longValue(), ChronoUnit.SECONDS);
			long nanos = seconds.multiply(billion).remainder(billion).longValue();
			range = range.plusNanos(nanos);
		}
		return range;
	}

	protected static <T extends Temporal & Comparable>boolean doASymRangeTemporal(T value, T compareTo, String s) {
		T start;
		T end;
		Duration range = getDuration(s);
		if (range.isNegative()) {
			start = (T) compareTo.plus(range);
			end = compareTo;
		}
		else {
			start = compareTo;
			end = (T) compareTo.plus(range);
		}
		return  value.compareTo(start)>=0 && value.compareTo(end)<=0;
	}

}
