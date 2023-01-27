package org.forwoods.messagematch.generate;

import java.util.Map;

import org.forwoods.messagematch.generate.nodegenerators.*;
import org.forwoods.messagematch.generate.nodegenerators.constraints.ComparatorConstraint;
import org.forwoods.messagematch.generate.nodegenerators.constraints.ProvidedConstraint;
import org.forwoods.messagematch.matchgrammar.MatcherBaseListener;
import org.forwoods.messagematch.matchgrammar.MatcherParser.GenValueContext;
import org.forwoods.messagematch.matchgrammar.MatcherParser.RegexpMatcherContext;
import org.forwoods.messagematch.matchgrammar.MatcherParser.TypeMatcherContext;

public class GrammarListenerGenerator extends MatcherBaseListener {

	NodeGenerator result;
	private final Map<String, ValueProvider> bindings;

	public GrammarListenerGenerator(Map<String, ValueProvider> bindings) {
		this.bindings = bindings;
	}

	@Override
	public void exitTypeMatcher(TypeMatcherContext ctx) {
		String type = ctx.type.getText();
		GenValueContext defaultVal = ctx.genValue();
		ValueProvider provider;
		if (ctx.binding() == null) {
			provider = new ValueProvider();
		} else {
			String binding = ctx.binding().IDENTIFIER().getText();
			provider = bindings.computeIfAbsent(binding, b->new ValueProvider());
		}
		if (defaultVal != null) {
			String genVal = defaultVal.getText().substring(1);// remove ','
			provider.addConstraint(new ProvidedConstraint(genVal));
		}
		if(ctx.comp!=null) {
			provider.addConstraint(new ComparatorConstraint(ctx.comp, bindings, type));
		}
		switch (type) {
		case "$Int":
			result = new IntTypeGenerator(provider);
			break;
		case "$String" :
			result = new StringTypeGenerator(provider);
			break;
		case "$Num" :
			result = new NumberTypeGenerator(provider);
			break;
		case "$Instant" :
			result = new InstantTypeGenerator(provider, bindings.get("datetime").asInt());
			break;
		case "$Date" :
			result = new DateTypeGenerator(provider, bindings.get("date").asString());
			break;
		case "$Time" :
			result = new TimeTypeGenerator(provider, bindings.get("time").asString());
			break;
		case "$Boolean" :
			result = new BoolTypeGenerator(provider);
			break;
		default:
			throw new UnsupportedOperationException("Cant match against type "+type);
		}
	}

	@Override
	public void exitRegexpMatcher(RegexpMatcherContext ctx) {
		ValueProvider provider;
		if (ctx.binding() == null) {
			provider = new ValueProvider();
		} else {
			String binding = ctx.binding().IDENTIFIER().getText();
			provider = bindings.computeIfAbsent(binding, b->new ValueProvider());
		}
		if (ctx.genValue() != null) {
			String genVal = ctx.genValue().getText().substring(1);// remove ','
			provider.addConstraint(new ProvidedConstraint(genVal));
		}
		result = new StringTypeGenerator(provider);
	}


}
