package org.forwoods.messagematch.generate.nodegenerators.constraints;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalTime;

public interface Constraint {

    boolean matches(Object o);

    Object generate();

    Constraint intConstraint = new Constraint() {
        @Override
        public boolean matches(Object o) {
            return o instanceof BigInteger;
        }

        @Override
        public Object generate() {
            return BigInteger.valueOf(0);
        }
    };

    Constraint numberConstraint = new Constraint() {
        @Override
        public boolean matches(Object o) {
            return o instanceof Number;
        }

        @Override
        public Object generate() {
            return BigDecimal.valueOf(0.0);
        }
    };

    Constraint stringConstraint = new Constraint() {
        @Override
        public boolean matches(Object o) {
            return true;
        }

        @Override
        public Object generate() {
            return "string";
        }

    };

    static Constraint instantConstraint(BigInteger instant) {
        return new Constraint() {
            @Override
            public boolean matches(Object o) {
                return o instanceof BigInteger;
            }

            @Override
            public Object generate() {
                return instant;
            }
        };
    }

    static Constraint dateConstraint(String localDate) {
        return new Constraint() {
            @Override
            public boolean matches(Object o) {
                try {
                    LocalDate.parse(o.toString());
                    return true;
                } catch (Exception e) {
                }
                return false;
            }

            @Override
            public Object generate() {
                return localDate;
            }
        };
    }

    static Constraint timeConstraint(String localTime) {
        return new Constraint() {
            @Override
            public boolean matches(Object o) {
                try {
                    LocalTime.parse(o.toString());
                    return true;
                } catch (Exception e) {
                }
                return false;
            }

            @Override
            public Object generate() {
                return localTime;
            }
        };
    }

}
