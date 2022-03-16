package org.forwoods.messagematch.generate;

import java.util.Map;

import org.forwoods.messagematch.generate.nodegenerators.DateTypeGenerator;
import org.forwoods.messagematch.generate.nodegenerators.InstantTypeGenerator;
import org.forwoods.messagematch.generate.nodegenerators.IntTypeGenerator;
import org.forwoods.messagematch.generate.nodegenerators.NodeGenerator;
import org.forwoods.messagematch.generate.nodegenerators.NumberTypeGenerator;
import org.forwoods.messagematch.generate.nodegenerators.StringTypeGenerator;
import org.forwoods.messagematch.generate.nodegenerators.TimeTypeGenerator;
import org.forwoods.messagematch.generate.nodegenerators.ValueProvider;
import org.forwoods.messagematch.generate.nodegenerators.constraints.ComparatorConstraint;
import org.forwoods.messagematch.generate.nodegenerators.constraints.ProvidedConstraint;
import org.forwoods.messagematch.matchgrammar.MatcherBaseListener;
import org.forwoods.messagematch.matchgrammar.MatcherParser.GenValueContext;
import org.forwoods.messagematch.matchgrammar.MatcherParser.RegexpMatcherContext;
import org.forwoods.messagematch.matchgrammar.MatcherParser.TypeMatcherContext;

/**
 * @author Tom
 *
 */
public class GrammarListenerGenerator extends MatcherBaseListener {

	NodeGenerator result;
	private Map<String, ValueProvider> bindings;

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
			provider = bindings.computeIfAbsent(binding, b->new ValueProvider(b));
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
			provider = bindings.computeIfAbsent(binding, b->new ValueProvider(b));
		}
		if (ctx.genValue() != null) {
			String genVal = ctx.genValue().getText().substring(1);// remove ','
			provider.addConstraint(new ProvidedConstraint(genVal));
		}
		result = new StringTypeGenerator(provider);
	}


}
