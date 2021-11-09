package org.forwoods.messagematch.messagematch.match.fieldmatchers;

public abstract class BoundsMatcher<X extends Comparable<X>> extends FieldMatcher {

	protected X expected;
	protected String operator;

	public BoundsMatcher(String operator, X expected, String binding) {
		super(binding);
		this.expected = expected;
		this.operator = operator;
	}

	protected boolean basicComp(X val) {
		int comp = val.compareTo(expected);
		switch (operator) {
		case ">":
			return comp > 0;
		case "<":
			return comp < 0;
		case ">=":
			return comp >= 0;
		case "<=":
			return comp <= 0;
		default:
			return false;
		}
	}

}