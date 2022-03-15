package org.forwoods.messagematch.match.fieldmatchers;

import java.math.BigInteger;
import java.util.regex.Pattern;

public class IntTypeMatcher extends FieldMatcher<BigInteger> {

	final static Pattern intPattern = Pattern.compile("-?[0-9]+");
	private FieldComparatorMatcher comparator;

	public IntTypeMatcher(String binding, boolean nullable, FieldComparatorMatcher comparator) {
		super(binding, nullable, comparator);
		this.comparator = comparator;
	}

	@Override
	protected BigInteger asComparable(String val) {
		return new BigInteger(val);
	}


	@Override
	boolean doMatch(String value) {
		boolean test = intPattern.asMatchPredicate().test(value);
		return test;
	}

	@Override
	public boolean doSymRange(BigInteger value, BigInteger compareTo, String s) {
		BigInteger range = new BigInteger(s).abs();
		BigInteger diff = compareTo.subtract(value).abs();
		return diff.compareTo(range) <=0;
	}

	@Override
	public boolean doASymRange(BigInteger value, BigInteger compareTo, String s) {
		BigInteger range = new BigInteger(s);
		BigInteger min;
		BigInteger max;
		if (range.signum()<0) {
			min = compareTo.add(range);
			max  =compareTo;
		}
		else
		{
			min = compareTo;
			max = compareTo.add(range);
		}
		return value.compareTo(min)>=0 && value.compareTo(max)<=0;
	}

}
