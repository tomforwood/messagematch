package org.forwoods.messagematch.messagematch.match.fieldmatchers;

import java.time.temporal.Temporal;

public class TimeBoundsMatcher<X extends Temporal & Comparable<X>> extends BoundsMatcher<X> {

	private long eta;
	//TODO this class is supposed to allow us to specify a date is within n days or a time withing n seconds etc
	public TimeBoundsMatcher(String operator, X v, long x, String binding) {
		super(operator, v,binding);
		this.eta = x;
		
	}

	@Override
	boolean doMatch(String value) {
		Temporal val = parseTemporal(value);
		//if (operator.equals("+-")) {
		return false;
	}

	private X parseTemporal(String value) {
		return null;//if (expected.getClass().equals(value))
	}

}
