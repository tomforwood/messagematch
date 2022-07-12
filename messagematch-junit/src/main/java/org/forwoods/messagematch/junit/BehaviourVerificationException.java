package org.forwoods.messagematch.junit;

public class BehaviourVerificationException extends RuntimeException {
    public BehaviourVerificationException(String message) {
        super(message);
    }

    public BehaviourVerificationException(String message, Error e) {
        super(message, e);
    }
}
