package org.forwoods.messagematch.match;

public class UnboundVariableException extends RuntimeException {

	private final String var;

	public UnboundVariableException(String var) {
		this.var = var;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getVar() {
		return var;
	}

}
