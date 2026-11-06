package io.smallrye.faulttolerance.core.metrics;

/**
 * Metadata of the metered operation that the guard itself doesn't know, because it comes
 * from the interception point. Notably, a method annotated {@code @ApplyGuard} may also
 * declare {@code @Fallback}, in which case the fallback is injected into the
 * {@link io.smallrye.faulttolerance.core.FaultToleranceContext} and the guard's own
 * metered operation doesn't know about it.
 */
public final class MeteredOperationMetadata {
    private final String name;
    private final boolean hasFallback;

    public MeteredOperationMetadata(String name, boolean hasFallback) {
        this.name = name;
        this.hasFallback = hasFallback;
    }

    public String name() {
        return name;
    }

    public boolean hasFallback() {
        return hasFallback;
    }
}
