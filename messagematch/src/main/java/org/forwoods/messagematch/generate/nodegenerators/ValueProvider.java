package org.forwoods.messagematch.generate.nodegenerators;

import org.forwoods.messagematch.generate.nodegenerators.constraints.Constraint;
import org.forwoods.messagematch.generate.nodegenerators.constraints.ProvidedConstraint;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class ValueProvider {

    private final String name;
    Set<Constraint> constraints = new HashSet<>();

    private Object value;

    public ValueProvider(String key) {
        this.name = key;
    }

    public ValueProvider() {
        this.name = null;
    }

    public ValueProvider(Constraint c) {
        this.name=null;
        constraints.add(c);
    }

    public Object generate() {
        if (value!=null) return value;

        Set<Object> possibles = constraints.stream().map(c->c.generate()).collect(Collectors.toSet());

        value = possibles.stream().filter(o->constraints.stream().allMatch(c->c.matches(o))).findAny().orElseThrow(()->new RuntimeException("Unable to generate value that  matches"));
        return value;
    }

    public void addConstraint(Constraint constraint) {
        constraints.add(constraint);
    }

    public BigInteger asInt() {
        return (BigInteger) generate();//the constraint will have ensured this is safe...
    }

    public BigDecimal asNum() {
        Object generate = generate();
        if (generate instanceof BigDecimal) return (BigDecimal) generate;
        return new BigDecimal((BigInteger) generate);
    }

    public String asString() {
        return generate().toString();
    }

    public String asDate() {
        return generate().toString();
    }

    public String asInstant() {
        return Instant.ofEpochMilli(asInt().longValue()).toString();
    }

    public String asTime() {
        return generate().toString();
    }
}
