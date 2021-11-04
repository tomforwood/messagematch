package org.forwoods.messagematch.messagematch.match;

import java.math.BigDecimal;

public class BoundsMatcher extends FieldMatcher {

	private BigDecimal expected;
	private BigDecimal eta;
	private String operator;

	public BoundsMatcher(String operator, BigDecimal v, BigDecimal x, String binding) {
		super(binding);
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
			int comp=val.compareTo(expected);
			switch(operator) {
			case ">": return comp > 0;
			case "<": return comp < 0;
			case ">=": return comp >=0;
			case "<=": return comp <=0;
			default: return false;
			}
		}
	}

}
