package org.forwoods.messagematch.messagematch.match;

import java.math.BigDecimal;
import java.util.Map;

import org.forwoods.messagematch.messagematch.matchgrammar.MatcherBaseListener;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser.BoundsMatcherContext;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser.NumOrVarContext;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser.RegexpMatcherContext;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser.TypeMatcherContext;

public class GrammarListenerMatcher extends MatcherBaseListener {

	FieldMatcher result;
	private final Map<String, String> bindings;
	
	public GrammarListenerMatcher(Map<String, String> bindings) {
		super();
		this.bindings = bindings;
	}

	@Override
	public void exitTypeMatcher(TypeMatcherContext ctx) {
		String type = ctx.type.getText();
		String binding = ctx.binding()==null?null:ctx.binding().IDENTIFIER().getText();
		switch (type) {
		case "Int":
			result = new IntTypeMatcher(binding);
			break;
			default:
				throw new UnsupportedOperationException("Cant match against type");
		}
	}

	@Override
	public void exitRegexpMatcher(RegexpMatcherContext ctx) {
		String regexp;
		String text = ctx.RE().getText();
		String binding = ctx.binding()==null?null:ctx.binding().getText();
		regexp = text.substring(1, text.length()-1);//strip off the delimiters
		regexp = regexp.replaceAll("\\\\\\^", "^");//and unescape
		result = new RegExpMatcher(regexp, binding);
	}

	@Override
	public void exitBoundsMatcher(BoundsMatcherContext ctx) {
		String operator = ctx.op.getText();
		String binding = ctx.binding()==null?null:ctx.binding().IDENTIFIER().getText();

		String value=null;
		NumOrVarContext val = ctx.val;
		if (val.NUMBER()!=null) {
			value = val.NUMBER().getText();
		}
		else {
			String var = val.variable().IDENTIFIER().getText();
			value = bindings.get(var);
			if (value==null) throw new UnboundVariableException(var);
		}
		if (operator.equals("+-")) {
			String eta = ctx.eta.getText();
			BigDecimal v = new BigDecimal(value);
			BigDecimal x = new BigDecimal(eta);
			result =  new BoundsMatcher(operator, v, x, binding);
		}
		else {
			BigDecimal v = new BigDecimal(value);
			result =  new BoundsMatcher(operator, v, null, binding);
		}
	}
	
	


}
