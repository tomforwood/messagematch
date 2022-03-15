package org.forwoods.messagematch.match.fieldmatchers;

public class StringTypeMatcher extends FieldMatcher<String> {

	public StringTypeMatcher(String binding, boolean nullable, FieldComparatorMatcher comparator) {
		super(binding, nullable, comparator);
	}

	@Override
	protected String asComparable(String val) {
		return val;
	}

	@Override
	boolean doMatch(String value) {
		return true;
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
