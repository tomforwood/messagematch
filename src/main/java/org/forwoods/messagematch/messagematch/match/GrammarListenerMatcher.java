package org.forwoods.messagematch.messagematch.match;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import org.forwoods.messagematch.messagematch.match.fieldmatchers.DateTypeMatcher;
import org.forwoods.messagematch.messagematch.match.fieldmatchers.DecimalBoundsMatcher;
import org.forwoods.messagematch.messagematch.match.fieldmatchers.FieldMatcher;
import org.forwoods.messagematch.messagematch.match.fieldmatchers.InstantTypeMatcher;
import org.forwoods.messagematch.messagematch.match.fieldmatchers.IntTypeMatcher;
import org.forwoods.messagematch.messagematch.match.fieldmatchers.NumTypeMatcher;
import org.forwoods.messagematch.messagematch.match.fieldmatchers.RegExpMatcher;
import org.forwoods.messagematch.messagematch.match.fieldmatchers.StringTypeMatcher;
import org.forwoods.messagematch.messagematch.match.fieldmatchers.TimeTypeMatcher;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherBaseListener;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser.BoundsMatcherContext;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser.NumOrVarContext;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser.RegexpMatcherContext;
import org.forwoods.messagematch.messagematch.matchgrammar.MatcherParser.TypeMatcherContext;

public class GrammarListenerMatcher extends MatcherBaseListener {

	FieldMatcher result;
	private final Map<String, Object> bindings;

	public GrammarListenerMatcher(Map<String, Object> bindings) {
		super();
		this.bindings = bindings;
	}

	@Override
	public void exitTypeMatcher(TypeMatcherContext ctx) {
		String type = ctx.type.getText();
		String binding = ctx.binding() == null ? null : ctx.binding().IDENTIFIER().getText();
		switch (type) {
		case "$Int":
			result = new IntTypeMatcher(binding);
			break;
		case "$String":
			result = new StringTypeMatcher(binding);
			break;
		case "$Num":
			result = new NumTypeMatcher(binding);
			break;
		case "$Instant":
			result = new InstantTypeMatcher(binding);
			break;
		case "$Date":
			result = new DateTypeMatcher(binding);
			break;
		case "$Time":
			result = new TimeTypeMatcher(binding);
			break;
		default:
			throw new UnsupportedOperationException("Cant match against type");
		}
	}

	@Override
	public void exitRegexpMatcher(RegexpMatcherContext ctx) {
		String regexp;
		String text = ctx.RE().getText();
		String binding = ctx.binding() == null ? null : ctx.binding().getText();
		regexp = text.substring(1, text.length() - 1);// strip off the delimiters
		regexp = regexp.replaceAll("\\\\\\^", "^");// and unescape
		result = new RegExpMatcher(regexp, binding);
	}

	@Override
	public void exitBoundsMatcher(BoundsMatcherContext ctx) {
		String operator = ctx.op.getText();
		String binding = ctx.binding() == null ? null : ctx.binding().IDENTIFIER().getText();

		BigDecimal value = null;
		NumOrVarContext val = ctx.val;
		if (val.NUMBER() != null) {
			value = trans(val.NUMBER().getText());
		} else {
			String var = val.variable().IDENTIFIER().getText();
			value = trans(bindings.get(var));
			if (value == null)
				throw new UnboundVariableException(var);
		}
		if (operator.equals("+-")) {
			String eta = ctx.eta.getText();
			BigDecimal x = new BigDecimal(eta);
			result = new DecimalBoundsMatcher(operator, value, x, binding);
		} else {
			result = new DecimalBoundsMatcher(operator, value, null, binding);
		}
	}

	private BigDecimal trans(Object object) {
		if (object instanceof String) {
			try {
				return new BigDecimal(object.toString());
			} catch (NumberFormatException e) {
				// not a number - lets try a date
				ZonedDateTime z = ZonedDateTime.parse(object.toString());
				return new BigDecimal(z.toInstant().toEpochMilli());
			}
		}
		if (object instanceof ZonedDateTime) {
			// the value can be represented a ms or ISO 8601
			return new BigDecimal(((ZonedDateTime) object).toInstant().toEpochMilli());
		}
		if (object instanceof LocalDate) {
			ZonedDateTime atStartOfDay = ((LocalDate) object).atStartOfDay(ZoneOffset.UTC);
			return new BigDecimal(atStartOfDay.toInstant().toEpochMilli());
		}
		if (object instanceof LocalTime) {
			ZonedDateTime atStartOfDay = ((LocalTime) object).atOffset(ZoneOffset.UTC).atDate(LocalDate.now())
					.toZonedDateTime();
			return new BigDecimal(atStartOfDay.toInstant().toEpochMilli());
		}
		if (object==null) return null;
		return new BigDecimal(object.toString());

	}

}
