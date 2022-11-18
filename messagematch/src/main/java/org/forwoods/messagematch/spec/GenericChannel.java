package org.forwoods.messagematch.spec;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GenericChannel implements Channel {
	@JsonProperty("typeName")
	private String typeName;
	
	private final Map<String, String> properties = new HashMap<>();
	
	@JsonAnySetter
	public void add(String key, String value) {
		properties.put(key, value);
	}
	
	public String getTypeName() {
		return typeName;
	}
	
	public Map<String, String> getProperties() {
		return properties;
	}

	@Override
	public String toString() {
		return "GenericChannel{" +
				"typeName='" + typeName + '\'' +
				", properties=" + properties +
				'}';
	}
}
