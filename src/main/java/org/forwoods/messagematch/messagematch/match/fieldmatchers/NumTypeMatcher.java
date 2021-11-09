package org.forwoods.messagematch.messagematch.match.fieldmatchers;

import java.util.regex.Pattern;

public class NumTypeMatcher extends FieldMatcher {

	final static Pattern doublePattern = Pattern.compile("-?[0-9]+(\\.[0-9]+)?([Ee][+\\-]?[0-9]+)?");
	public NumTypeMatcher(String binding) {
		super(binding);
	}

	@Override
	boolean doMatch(String value) {
		return doublePattern.asMatchPredicate().test(value);
	}

}
