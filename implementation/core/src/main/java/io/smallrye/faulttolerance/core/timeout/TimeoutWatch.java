package io.smallrye.faulttolerance.core.timeout;

interface TimeoutWatch {
    boolean isRunning();

    void cancel();
}
