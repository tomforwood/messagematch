package org.forwoods.messagematch.messagematch.match;

public class UnboundVariableException extends RuntimeException {

	private final String var;

	public UnboundVariableException(String var) {
		this.var = var;
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String getVar() {
		return var;
	}

}
