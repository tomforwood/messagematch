package org.forwoods.messagematch.match.fieldmatchers;

import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Map;

public abstract class FieldMatcher<T extends Comparable<T>> {
	protected final String binding;
	protected final boolean nullable;
	private final FieldComparatorMatcher comparator;

	public FieldMatcher(String binding, boolean nullable, FieldComparatorMatcher comparator) {
		this.binding = binding;
		this.nullable = nullable;
		this.comparator = comparator;
	}

	public boolean matches(String value, Map<String, Object> bindings) {
		if (binding!=null) {
			Object existing = bindings.get(binding);
			if (existing!=null) {
				if (notEqual(value, existing)) {
					return false;
				}
			}
			bindings.put(binding, value);
		}
		if (value==null) return nullable;
		boolean match = doMatch(value);
		if (comparator!=null) {
			match = comparator.match(asComparable(value), bindings, this);
		}
		return match ;
	}

	protected abstract T asComparable(String val);

	private boolean notEqual(String value, Object existing) {
		if (existing instanceof String) return !existing.equals(value);
		if (existing instanceof Instant) {
			//the value can be represented a ms or ISO 8601
			try {
				long ms = Long.parseLong(value);
				long epochMilli = ((Instant)existing).toEpochMilli();
				return ms != epochMilli;
			}
			catch (NumberFormatException ignored) {}//it isn't a number so assume it is ISO
			return !existing.toString().equals(value);
		}
		if (existing instanceof Temporal) {
			//other time types are expected to be ISO 8601
			return !existing.toString().equals(value);
		}
		return false;
	}
	
	abstract boolean doMatch(String value);

	public abstract boolean doSymRange(T value, T compareTo, String s);
	public abstract boolean doASymRange(T value, T compareTo, String s);
}
