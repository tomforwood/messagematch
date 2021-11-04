package org.forwoods.messagematch.messagematch.generate;

import java.math.BigDecimal;

import org.forwoods.messagematch.messagematch.matchgrammar.MatcherBaseListener;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser.BoundsMatcherContext;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser.GenValueContext;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser.RegexpMatcherContext;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser.TypeMatcherContext;

public class GrammarListenerGenerator  extends MatcherBaseListener{
	
	NodeGenerator result;
	
	@Override
	public void exitTypeMatcher(TypeMatcherContext ctx) {
		String type = ctx.type.getText();
		GenValueContext defaultVal= ctx.genValue();
		String binding = ctx.binding()==null?null:ctx.binding().getText();
		String genVal="0";
		if (defaultVal!=null) {
			genVal = defaultVal.getText().substring(1);//remove ','
		}
		switch (type) {
		case "Int":
			result = new IntTypeGenerator(genVal, binding);
			break;
			default:
				throw new UnsupportedOperationException("Cant match against type");
		}
	}
	
	@Override
	public void exitRegexpMatcher(RegexpMatcherContext ctx) {
		String text = ctx.genValue().getText();
		String binding = ctx.binding()==null?null:ctx.binding().getText();
		result = new TextGenerator(text.substring(1), binding);//remove ','
	}
	
	@Override
	public void exitBoundsMatcher(BoundsMatcherContext ctx) {
		String operator = ctx.op.getText();
		String binding = ctx.binding()==null?null:ctx.binding().getText();
		switch (operator) {
		case "+-":
			String value = ctx.numOrVar().getText();
			result = new NumberTypeGenerator(value, binding);
			break;
		case ">=":
			result = compGen(ctx, BigDecimal.ZERO, binding);
			break;
		case ">":
			result = compGen(ctx, BigDecimal.ONE, binding);
			break;
		case "<":
			result = compGen(ctx, BigDecimal.valueOf(-1), binding);
			break;
		case "<=":
			result = compGen(ctx, BigDecimal.ZERO, binding);
			break;
		default:
			throw new UnsupportedOperationException("operator "+operator+" not supported");
		}
	}
	
	NumberTypeGenerator compGen(BoundsMatcherContext ctx, BigDecimal diff, String binding) {
		String value = ctx.val.getText();
		BigDecimal val = new BigDecimal(value);
		val = val.add(diff);
		return new NumberTypeGenerator(val, binding);
	}

}
