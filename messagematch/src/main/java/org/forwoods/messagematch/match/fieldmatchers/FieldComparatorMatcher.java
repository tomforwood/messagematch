package org.forwoods.messagematch.match.fieldmatchers;

import org.forwoods.messagematch.matchgrammar.MatcherParser;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class FieldComparatorMatcher extends FieldComparator {
    public FieldComparatorMatcher(MatcherParser.ComparatorContext comp) {
        super(comp);
    }

    public <T extends Comparable<T>> boolean match(T value, Map<String, Object> bindings, FieldMatcher<T> matcher) {

        T compareTo;
        Function<String, T> f = matcher::asComparable;
        compareTo = vvToComp(value, bindings, f);//, matcher::asComparable, val);


        return compare(value, matcher, compareTo, op, eta);
    }

    public static <T extends Comparable<T>> boolean compare(T value, FieldMatcher<T> matcher, T compareTo, String op, Optional<String> eta) {
        switch (op) {
            case "++":
                return matcher.doASymRange(value, compareTo, eta.orElse("0"));
            case "+-":
                return matcher.doSymRange(value, compareTo, eta.orElse("0"));
            default:
                return basicComp(value, compareTo, op);
        }
    }

    public static <T extends Comparable<T>> boolean basicComp(T val, T compareVal, String op) {
        int comp = val.compareTo(compareVal);
        switch (op) {
            case ">":
                return comp > 0;
            case "<":
                return comp < 0;
            case ">=":
                return comp >= 0;
            case "<=":
                return comp <= 0;
            default:
                return false;
        }
    }

}
