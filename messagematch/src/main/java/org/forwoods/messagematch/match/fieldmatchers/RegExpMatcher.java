package org.forwoods.messagematch.match.fieldmatchers;

import java.util.regex.Pattern;

public class RegExpMatcher extends FieldMatcher<String> {
    private Pattern pattern;

	public RegExpMatcher(String regExp, String binding, boolean nullable, FieldComparatorMatcher comparator) {
		super(binding, nullable, comparator);
    	this.pattern = Pattern.compile(regExp);
    }

	@Override
	protected String asComparable(String val) {
		return val;
	}

	@Override
	public boolean doMatch(String value) {
		return pattern.asMatchPredicate().test(value);
	}


	@Override
	public boolean doSymRange(String value, String compareTo, String s) {
		throw new UnsupportedOperationException("This doesn't really make sense");
	}

	@Override
	public boolean doASymRange(String value, String compareTo, String s) {
		throw new UnsupportedOperationException("This doesn't really make sense");
	}
}
