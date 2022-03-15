package org.forwoods.messagematch.match.fieldmatchers;

import org.forwoods.messagematch.matchgrammar.MatcherParser;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class FieldComparatorMatcher extends FieldComparator {
    public FieldComparatorMatcher(MatcherParser.ComparatorContext comp) {
        super(comp);
    }

    public <T extends Comparable<T>> boolean match(T value, Map<String, Object> bindings, FieldMatcher matcher) {

        Comparable<T> compareTo;
        Function<String, Comparable<T>> f = matcher::asComparable;
        compareTo = vvToComp(value, bindings, f);//, matcher::asComparable, val);


        return compare(value, matcher, compareTo, op, eta);
    }

    public static <T extends Comparable<T>> boolean compare(Comparable<T> value, FieldMatcher matcher, Comparable<T> compareTo, String op, Optional<String> eta) {
        switch (op) {
            case "++":
                return matcher.doASymRange(value, compareTo, eta.get());
            case "+-":
                return matcher.doSymRange(value, compareTo, eta.get());
            default:
                return basicComp(value, compareTo, op);
        }
    }

    public static <T> boolean basicComp(Comparable<T> val, Comparable<T> compareVal, String op) {
        int comp = val.compareTo((T) compareVal);
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
