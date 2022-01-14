package io.smallrye.faulttolerance.fallback.causechain;

public class ExpectedOutcomeException extends Exception {
    public ExpectedOutcomeException() {
    }

    public ExpectedOutcomeException(Throwable cause) {
        super(cause);
    }
}
