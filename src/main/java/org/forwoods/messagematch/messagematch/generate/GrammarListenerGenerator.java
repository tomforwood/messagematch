package org.forwoods.messagematch.messagematch.generate;

import java.math.BigDecimal;

import org.forwoods.messagematch.messagematch.matchgrammar.MatcherBaseListener;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser.BoundsMatcherContext;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser.RegexpMatcherContext;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser.TypeMatcherContext;

public class GrammarListenerGenerator  extends MatcherBaseListener{
	
	NodeGenerator result;
	
	@Override
	public void exitTypeMatcher(TypeMatcherContext ctx) {
		String type = ctx.getChild(0).getText();
		String defaultVal= null;
		if (ctx.getChildCount()>1) {
			defaultVal = ctx.getChild(1).getText();
		}
		switch (type) {
		case "Int":
			result = new IntTypeGenerator(defaultVal);
			break;
			default:
				throw new UnsupportedOperationException("Cant match against type");
		}
	}
	
	@Override
	public void exitRegexpMatcher(RegexpMatcherContext ctx) {
		String regexp;
		String text = ctx.getChild(1).getText();
		result = new TextGenerator(text.substring(1));
	}
	
	@Override
	public void exitBoundsMatcher(BoundsMatcherContext ctx) {
		String operator = ctx.getChild(0).getText();
		switch (operator) {
		case "+-":
			String value = ctx.getChild(2).getText();
			result = new NumberTypeGenerator(value);
			break;
		case ">=":
			result = compGen(ctx, BigDecimal.ZERO);
			break;
		case ">":
			result = compGen(ctx, BigDecimal.ONE);
			break;
		case "<":
			result = compGen(ctx, BigDecimal.valueOf(-1));
			break;
		case "<=":
			result = compGen(ctx, BigDecimal.ZERO);
			break;
		default:
			throw new UnsupportedOperationException("operator "+operator+" not supported");
		}
	}
	
	NumberTypeGenerator compGen(BoundsMatcherContext ctx, BigDecimal diff) {
		String value = ctx.getChild(1).getText();
		BigDecimal val = new BigDecimal(value);
		val = val.add(diff);
		return new NumberTypeGenerator(val);
	}

}
