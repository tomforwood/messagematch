package org.forwoods.messagematch.messagematch.match.fieldmatchers;

import java.time.Instant;
import java.time.temporal.Temporal;
import java.util.Map;

public abstract class FieldMatcher {
	protected final String binding;

	public FieldMatcher(String binding) {
		this.binding = binding;
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
		return doMatch(value);
	}

	private boolean notEqual(String value, Object existing) {
		if (existing instanceof String) return !existing.equals(value);
		if (existing instanceof Instant) {
			//the value can be represented a ms or ISO 8601
			try {
				long ms = Long.parseLong(value);
				long epochMilli = ((Instant)existing).toEpochMilli();
				return ms != epochMilli;
			}
			catch (NumberFormatException e) {}//it isn't a number so assume it is ISO
			return !existing.toString().equals(value);
		}
		if (existing instanceof Temporal) {
			//other time types are expected to be ISO 8601
			return !existing.toString().equals(value);
		}
		return false;
	}
	
	abstract boolean doMatch(String value);
}
