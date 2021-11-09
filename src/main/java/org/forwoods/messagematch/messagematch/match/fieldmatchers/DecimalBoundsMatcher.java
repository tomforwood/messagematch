package org.forwoods.messagematch.messagematch.match.fieldmatchers;

import java.math.BigDecimal;

public class DecimalBoundsMatcher extends BoundsMatcher<BigDecimal> {

	private BigDecimal expected;
	private BigDecimal eta;
	private String operator;

	public DecimalBoundsMatcher(String operator, BigDecimal v, BigDecimal x, String binding) {
		super(operator, v, binding);
		this.operator = operator;
		this.expected = v;
		this.eta = x;
		
	}

	@Override
	boolean doMatch(String value) {
		BigDecimal val = new BigDecimal(value);
		if (operator.equals("+-")) {
		BigDecimal diff = val.subtract(expected).abs();
		return diff.compareTo(eta.abs()) <= 0;
		}
		else {
			return basicComp(val);
		}
	}

}
