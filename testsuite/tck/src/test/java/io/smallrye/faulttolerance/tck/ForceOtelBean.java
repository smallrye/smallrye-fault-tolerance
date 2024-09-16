package io.smallrye.faulttolerance.tck;

import jakarta.enterprise.inject.spi.CDI;

import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.Before;

import io.opentelemetry.api.OpenTelemetry;

public class ForceOtelBean {
    public void beforeEachTest(@Observes Before event) {
        if (event.getTestClass().getName().startsWith("org.eclipse.microprofile.fault.tolerance.tck.telemetryMetrics.")) {
            // TODO make sure the `OpenTelemetry` bean is instantiated eagerly, which in turn triggers
            //  initialization of the MP FT TCK supporting infrastructure, which is required in the tests
            CDI.current().select(OpenTelemetry.class).get();
        }
    }
}
