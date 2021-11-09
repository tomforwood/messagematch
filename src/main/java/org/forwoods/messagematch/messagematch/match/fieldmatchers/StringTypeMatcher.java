package org.forwoods.messagematch.messagematch.match.fieldmatchers;

public class StringTypeMatcher extends FieldMatcher {

	public StringTypeMatcher(String binding) {
		super(binding);
	}

	@Override
	boolean doMatch(String value) {
		return true;
	}

}
