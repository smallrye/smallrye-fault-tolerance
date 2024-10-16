package io.smallrye.faulttolerance.core.metrics;

public final class MeteredOperationName {
    private final String name;

    public MeteredOperationName(String name) {
        this.name = name;
    }

    public String get() {
        return name;
    }
}
