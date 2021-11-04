package org.forwoods.messagematch.messagematch.match;

import java.math.BigInteger;
import java.util.regex.Pattern;

public class IntTypeMatcher implements FieldMatcher {

	Pattern intPattern = Pattern.compile("-?[0-9]*");
	
	@Override
	public boolean matches(String value) {
		return intPattern.asMatchPredicate().test(value);
	}

}
