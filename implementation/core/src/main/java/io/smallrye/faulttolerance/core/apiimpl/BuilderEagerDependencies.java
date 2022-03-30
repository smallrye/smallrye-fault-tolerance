package io.smallrye.faulttolerance.core.apiimpl;

// dependencies that may be accessed eagerly; these must be safe to use during static initialization
public interface BuilderEagerDependencies {
    BasicCircuitBreakerMaintenanceImpl cbMaintenance();
}
