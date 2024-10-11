package io.smallrye.faulttolerance.tck;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;

import io.smallrye.opentelemetry.implementation.config.OpenTelemetryConfigProducer;

public class OtelConfigExtension implements Extension {
    public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
        bbd.addAnnotatedType(OpenTelemetryConfigProducer.class, OpenTelemetryConfigProducer.class.getName());
    }
}
