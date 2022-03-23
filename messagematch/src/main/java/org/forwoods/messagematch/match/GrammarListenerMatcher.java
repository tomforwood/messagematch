package org.forwoods.messagematch.match;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;

import org.forwoods.messagematch.match.fieldmatchers.*;
import org.forwoods.messagematch.matchgrammar.MatcherBaseListener;
import org.forwoods.messagematch.matchgrammar.MatcherParser.RegexpMatcherContext;
import org.forwoods.messagematch.matchgrammar.MatcherParser.TypeMatcherContext;

public class GrammarListenerMatcher extends MatcherBaseListener {

	FieldMatcher<?> result;


	public GrammarListenerMatcher(Map<String, Object> bindings) {
		super();
	}

	@Override
	public void exitTypeMatcher(TypeMatcherContext ctx) {
		String type = ctx.type.getText();
		String binding = ctx.binding() == null ? null : ctx.binding().IDENTIFIER().getText();
		boolean nullable = ctx.nullable!=null;
		FieldComparatorMatcher comparator = ctx.comp==null?null:new FieldComparatorMatcher(ctx.comp);
		switch (type) {
		case "$Int":
			result = new IntTypeMatcher(binding, nullable, comparator);
			break;
		case "$String":
			result = new StringTypeMatcher(binding, nullable, comparator);
			break;
		case "$Num":
			result = new NumTypeMatcher(binding, nullable, comparator);
			break;
		case "$Instant":
			result = new InstantTypeMatcher(binding, nullable, comparator);
			break;
		case "$Date":
			result = new DateTypeMatcher(binding, nullable, comparator);
			break;
		case "$Time":
			result = new TimeTypeMatcher(binding, nullable, comparator);
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
		result = new RegExpMatcher(regexp, binding, false, null);
	}

	public static BigDecimal trans(Object object) {
		if (object instanceof BigDecimal) {
			return (BigDecimal) object;
		}
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
