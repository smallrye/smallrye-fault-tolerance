package io.smallrye.faulttolerance.standalone;

import io.smallrye.faulttolerance.core.apiimpl.BasicCircuitBreakerMaintenanceImpl;
import io.smallrye.faulttolerance.core.apiimpl.BuilderEagerDependencies;

final class EagerDependencies implements BuilderEagerDependencies {
    final BasicCircuitBreakerMaintenanceImpl cbMaintenance = new BasicCircuitBreakerMaintenanceImpl();

    @Override
    public BasicCircuitBreakerMaintenanceImpl cbMaintenance() {
        return cbMaintenance;
    }
}
