package org.forwoods.messagematch.generate.nodegenerators.constraints;

import java.math.BigDecimal;
import java.math.BigInteger;

public class ProvidedConstraint implements Constraint{

    private final Object value;
    public ProvidedConstraint(String val) {
        Object valStore;
        try {
            valStore = new BigInteger(val);
        }
        catch (NumberFormatException e) {
            try {
                valStore = new BigDecimal(val);
            }
            catch (NumberFormatException e2) {
                valStore = val;
            }
        }
        value = valStore;
    }

    @Override
    public boolean matches(Object o) {
        return o.equals(value);
    }

    @Override
    public Object generate() {
        return value;
    }
}
