package org.forwoods.messagematch.messagematch.generate;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Comparator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NumericNode;

public class NumericNodeComparator implements Comparator<JsonNode> {

	@Override
	public int compare(JsonNode o1, JsonNode o2) {
		if (o1.equals(o2)) {
			return 0;
		}
		if ((o1 instanceof NumericNode) && (o2 instanceof NumericNode)) {
			if (o1.isIntegralNumber() && o2.isIntegralNumber()) {
				BigInteger b1 = new BigInteger( o1.asText());
				BigInteger b2 = new BigInteger( o1.asText());
				return b1.compareTo(b2);
			} else if (!o1.isIntegralNumber() && !o2.isIntegralNumber()) {

				BigDecimal b1 = new BigDecimal( o1.asText());
				BigDecimal b2 = new BigDecimal( o1.asText());
				return b1.compareTo(b2);
			}
		}
		return 1;
	}

}
