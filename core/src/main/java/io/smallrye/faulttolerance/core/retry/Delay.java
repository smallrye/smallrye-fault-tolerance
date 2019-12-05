package io.smallrye.faulttolerance.core.retry;

public interface Delay {
    void sleep() throws InterruptedException;

    Delay NONE = () -> {
    };
}
