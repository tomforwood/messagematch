package org.forwoods.messagematch.messagematch.match.fieldmatchers;

import java.util.regex.Pattern;

public class RegExpMatcher extends FieldMatcher {
    private Pattern pattern;

	public RegExpMatcher(String regExp, String binding) {
		super(binding);
    	this.pattern = Pattern.compile(regExp);
    }

	@Override
	public boolean doMatch(String value) {
		return pattern.asMatchPredicate().test(value);
	}
}
