package org.forwoods.messagematch.match;

public class JsonPath {
	private final String pathElement;
	private final JsonPath parent;
	public JsonPath(String pathElement, JsonPath parent) {
		super();
		this.pathElement = pathElement;
		this.parent = parent;
	}
	
	public String toString() {
		if (parent==null) {
			return pathElement;
		}
		return parent +":"+ pathElement;
	}

}
