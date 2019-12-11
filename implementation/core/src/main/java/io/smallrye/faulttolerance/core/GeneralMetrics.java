package io.smallrye.faulttolerance.core;

// TODO better name?
public interface GeneralMetrics {
    void invoked();

    void failed();
}
