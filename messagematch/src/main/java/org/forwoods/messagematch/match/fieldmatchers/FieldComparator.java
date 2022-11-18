package org.forwoods.messagematch.match.fieldmatchers;

import org.antlr.v4.runtime.Token;
import org.forwoods.messagematch.match.UnboundVariableException;
import org.forwoods.messagematch.matchgrammar.MatcherParser;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class FieldComparator {
    protected final String op;
    protected final ValOrVar val;
    protected final Optional<String> eta;

    public FieldComparator(MatcherParser.ComparatorContext comp) {
        op = comp.op.getText();
        val = new ValOrVar(comp.val);
        eta = Optional.ofNullable(comp.eta).map(Token::getText);
    }

    protected <T extends Comparable<T>> T vvToComp(T value, Map<String, Object> bindings, Function<String, T> converter) {
        T compareTo;
        if (val.variable != null) {
            Object varVal = bindings.get(val.variable);
            if (varVal == null) throw new UnboundVariableException(val.variable);
            if (!varVal.getClass().isAssignableFrom(value.getClass())) {
                compareTo = converter.apply(varVal.toString());
            } else {
                //noinspection unchecked
                compareTo = (T) varVal;
            }
        } else {
            compareTo = converter.apply(val.value);
        }
        return compareTo;
    }

    public static class ValOrVar {
        private final String value;
        private final String variable;

        public ValOrVar(MatcherParser.ValOrVarContext context) {
            if (context.literal() != null) {
                value = context.literal().getText();
            } else
                value = null;
            if (context.variable() != null) {
                variable = context.variable().IDENTIFIER().getText();
            } else {
                variable = null;
            }
        }

        public String getValue() {
            return value;
        }

        public String getVariable() {
            return variable;
        }
    }

    public String getOp() {
        return op;
    }

    public ValOrVar getVal() {
        return val;
    }

    public Optional<String> getEta() {
        return eta;
    }
}
