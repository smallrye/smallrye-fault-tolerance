package io.smallrye.faulttolerance;

final class ThrowableWrapper extends Exception {
    public ThrowableWrapper(Throwable cause) {
        super(cause);
    }

    Throwable wrappedThrowable() {
        return getCause();
    }
}
