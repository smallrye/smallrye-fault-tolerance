package io.smallrye.faulttolerance.standalone;

import io.smallrye.faulttolerance.apiimpl.BasicCircuitBreakerMaintenanceImpl;
import io.smallrye.faulttolerance.apiimpl.BuilderEagerDependencies;

final class EagerDependencies implements BuilderEagerDependencies {
    final BasicCircuitBreakerMaintenanceImpl cbMaintenance = new BasicCircuitBreakerMaintenanceImpl();

    @Override
    public BasicCircuitBreakerMaintenanceImpl cbMaintenance() {
        return cbMaintenance;
    }
}
