package org.forwoods.messagematch.match.fieldmatchers;

public class BoolTypeMatcher extends FieldMatcher<Boolean> {


	public BoolTypeMatcher(String binding, boolean nullable, FieldComparatorMatcher comparator) {
		super(binding, nullable, comparator);
	}

	@Override
	protected Boolean asComparable(String val) {
		return Boolean.valueOf(val);
	}


	@Override
	boolean doMatch(String value) {
		return isBool(value);
	}

	public static boolean isBool(String value) {
		return "true".equals(value) || "false".equals(value);
	}

	@Override
	public boolean doSymRange(Boolean value, Boolean compareTo, String s) {
		throw new UnsupportedOperationException("This doesn't really make sense");
	}

	@Override
	public boolean doASymRange(Boolean value, Boolean compareTo, String s) {
		throw new UnsupportedOperationException("This doesn't really make sense");
	}

}
