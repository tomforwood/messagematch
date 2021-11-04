package org.forwoods.messagematch.messagematch.match;

import java.math.BigDecimal;

import org.forwoods.messagematch.messagematch.matchgrammar.MatcherBaseListener;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser.BoundsMatcherContext;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser.RegexpMatcherContext;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser.TypeMatcherContext;

public class GrammarListenerMatcher extends MatcherBaseListener {

	FieldMatcher result;
	
	@Override
	public void exitTypeMatcher(TypeMatcherContext ctx) {
		String type = ctx.getChild(0).getText();
		switch (type) {
		case "Int":
			result = new IntTypeMatcher();
			break;
			default:
				throw new UnsupportedOperationException("Cant match against type");
		}
	}

	@Override
	public void exitRegexpMatcher(RegexpMatcherContext ctx) {
		String regexp;
		String text = ctx.getChild(0).getText();
		regexp = text.substring(1, text.length()-1);
		regexp = regexp.replaceAll("\\\\\\^", "^");
		result = new RegExpMatcher(regexp);
	}

	@Override
	public void exitBoundsMatcher(BoundsMatcherContext ctx) {
		String operator = ctx.getChild(0).getText();
		if (operator.equals("+-")) {
			String value = ctx.getChild(2).getText();
			String eta = ctx.getChild(4).getText();
			BigDecimal v = new BigDecimal(value);
			BigDecimal x = new BigDecimal(eta);
			result =  new BoundsMatcher(operator, v, x);
		}
		else {
			String value = ctx.getChild(1).getText();
			BigDecimal v = new BigDecimal(value);
			result =  new BoundsMatcher(operator, v, null);
		}
	}
	
	


}
