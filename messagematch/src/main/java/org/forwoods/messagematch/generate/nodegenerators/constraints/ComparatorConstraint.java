package org.forwoods.messagematch.generate.nodegenerators.constraints;

import org.forwoods.messagematch.generate.nodegenerators.*;
import org.forwoods.messagematch.match.fieldmatchers.FieldComparator;
import org.forwoods.messagematch.match.fieldmatchers.FieldComparatorMatcher;
import org.forwoods.messagematch.match.fieldmatchers.IntTypeMatcher;
import org.forwoods.messagematch.match.fieldmatchers.NumTypeMatcher;
import org.forwoods.messagematch.matchgrammar.MatcherParser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;

public class ComparatorConstraint implements Constraint{

    private final FieldComparator comparator;
    private final Map<String, ValueProvider> bindings;
    private final ComparatorBehaviour<?> behavior;

    public ComparatorConstraint(MatcherParser.ComparatorContext comp, Map<String, ValueProvider> bindings, String goalType) {
        comparator = new FieldComparator(comp);
        this.bindings = bindings;
        this.behavior = getBehaviour(goalType);

    }

    private ComparatorBehaviour<?> getBehaviour(String goalType) {
        switch (goalType) {
            case "$Instant" :
            case "$Int":
                return new IntegerBehaviour();
            case "$Num" :
                return new NumberBehaviour();
            case "$String" :

            case "$Date" :
            case "$Time" :
            default:
                throw new UnsupportedOperationException("Cant match against type "+goalType);
        }
    }

    @Override
    public boolean matches(Object generated) {
        Object compareTo = toVP(comparator).generate();
        Optional<String> eta = comparator.getEta();
        return behavior.matches(generated, compareTo, eta, comparator.getOp());
    }

    @Override
    public Object generate() {
        Object compareTo = toVP(comparator).generate();
        Optional<String> eta = comparator.getEta();
        return  behavior.generate(compareTo, eta, comparator.getOp());
    }

    private ValueProvider toVP(FieldComparator comparator) {
        FieldComparator.ValOrVar vv = comparator.getVal();
        if (vv.getValue() !=null) {
            return new ValueProvider(new ProvidedConstraint(vv.getValue()));
        }
        else {
            return bindings.computeIfAbsent(vv.getVariable(), b->new ValueProvider());
        }
    }

    abstract static class ComparatorBehaviour<T extends Comparable<T>> {
        public T generate(Object compareTo, Optional<String> eta, String op) {
            return generate(doCast(compareTo), eta, op);
        }
        public abstract T generate(T compareTo, Optional<String> eta, String op);
        public abstract T doCast(Object o);
        public abstract boolean matches(T val, T compareTo, Optional<String> eta, String op);
        public boolean matches(Object val, Object compareTo, Optional<String> eta, String op) {
            return matches(doCast(val), doCast(compareTo), eta, op);
        }
    }

    static class IntegerBehaviour extends ComparatorBehaviour<BigInteger> {
        @Override
        public BigInteger generate(BigInteger compareTo, Optional<String> eta, String op) {
            switch (op) {
                case ">=":
                case "<=":
                case "+-":
                    return compareTo;
                case ">":
                    return compareTo.add(BigInteger.ONE);
                case "<":
                    return compareTo.subtract(BigInteger.ONE);
                case "++":
                        return compareTo.add(new BigInteger(eta.orElse("0")));
                default:
                    return compareTo;
            }
        }

        @Override
        public BigInteger doCast(Object o) {
            return (BigInteger) o;
        }

        @Override
        public boolean matches(BigInteger val, BigInteger compareTo, Optional<String> eta, String op) {
            return FieldComparatorMatcher.compare(val, new IntTypeMatcher(null, false, null),compareTo,op, eta );
        }
    }

    static class NumberBehaviour extends ComparatorBehaviour<BigDecimal> {
        @Override
        public BigDecimal generate(BigDecimal compareTo, Optional<String> eta, String op) {
            switch (op) {
                case ">=":
                case "<=":
                case "+-":
                    return compareTo;
                case ">":
                    return compareTo.add(BigDecimal.ONE);
                case "<":
                    return compareTo.subtract(BigDecimal.ONE);
                case "++":
                    return compareTo.add(new BigDecimal(eta.orElse("0")));
                default:
                    return compareTo;
            }
        }

        @Override
        public BigDecimal doCast(Object o) {
            if (o instanceof BigInteger) return new BigDecimal((BigInteger) o);
            return (BigDecimal) o;
        }

        @Override
        public boolean matches(BigDecimal val, BigDecimal compareTo, Optional<String> eta, String op) {
            return FieldComparatorMatcher.compare(val, new NumTypeMatcher(null, false, null),compareTo,op, eta );
        }
    }
}
