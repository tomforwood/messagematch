package org.forwoods.messagematch.messagematch.match;

import java.util.regex.Pattern;

public class RegExpMatcher implements FieldMatcher {
    private Pattern pattern;

	public RegExpMatcher(String regExp) {
    	this.pattern = Pattern.compile(regExp);
    }

	@Override
	public boolean matches(String value) {
		return pattern.asMatchPredicate().test(value);
	}
}
