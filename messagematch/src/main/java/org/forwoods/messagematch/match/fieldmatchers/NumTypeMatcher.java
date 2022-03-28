package org.forwoods.messagematch.match.fieldmatchers;

import java.math.BigDecimal;
import java.util.regex.Pattern;

public class NumTypeMatcher extends FieldMatcher<BigDecimal> {

	final static Pattern doublePattern = Pattern.compile("-?[0-9]+(\\.[0-9]+)?([Ee][+\\-]?[0-9]+)?");
	public NumTypeMatcher(String binding, boolean nullable, FieldComparatorMatcher comparator) {
		super(binding, nullable, comparator);
	}

    public static boolean isNumber(String value) {
		return doublePattern.asMatchPredicate().test(value);
    }

    @Override
	protected BigDecimal asComparable(String val) {
		return new BigDecimal(val);
	}

	@Override
	boolean doMatch(String value) {
		return isNumber(value);
	}

	@Override
	public boolean doSymRange(BigDecimal value, BigDecimal compareTo, String s) {
		BigDecimal range = new BigDecimal(s).abs();
		BigDecimal diff = compareTo.subtract(value).abs();
		return diff.compareTo(range) <=0;
	}

	@Override
	public boolean doASymRange(BigDecimal value, BigDecimal compareTo, String s) {
		BigDecimal range = new BigDecimal(s);
		BigDecimal min;
		BigDecimal max;
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
