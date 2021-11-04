package org.forwoods.messagematch.messagematch.match;

import java.util.regex.Pattern;

public class IntTypeMatcher extends FieldMatcher {

	final static Pattern intPattern = Pattern.compile("-?[0-9]*");
	public IntTypeMatcher(String binding) {
		super(binding);
	}


	@Override
	boolean doMatch(String value) {
		
		return intPattern.asMatchPredicate().test(value);
	}

}
