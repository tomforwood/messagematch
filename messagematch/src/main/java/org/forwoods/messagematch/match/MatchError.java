package org.forwoods.messagematch.match;

public class MatchError {
	private final JsonPath path;
	private final String expected;
	private final String actual;
	public MatchError(JsonPath path, String expected, String actual) {
		super();
		this.path = path;
		this.expected = expected;
		this.actual = actual;
	}
	
	public String toString() {
		return "Error at "+path + " expected matching "+expected + " but was " + actual;
	}
}
