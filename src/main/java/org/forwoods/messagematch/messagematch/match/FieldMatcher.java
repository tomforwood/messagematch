package org.forwoods.messagematch.messagematch.match;

import java.util.Map;

public abstract class FieldMatcher {
	protected final String binding;

	public FieldMatcher(String binding) {
		this.binding = binding;
	}

	boolean matches(String value, Map<String, String> bindings) {
		if (binding!=null) {
			String existing = bindings.get(binding);
			if (existing!=null) {
				if (!value.equals(existing)) {
					return false;
				}
			}
			bindings.put(binding, value);
		}
		return doMatch(value);
	}
	
	abstract boolean doMatch(String value);
}
