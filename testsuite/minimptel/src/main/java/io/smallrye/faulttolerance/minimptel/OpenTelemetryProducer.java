package io.smallrye.faulttolerance.minimptel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.Config;

import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;

@Singleton
public class OpenTelemetryProducer {
    private static final String NAME = "io.smallrye.faulttolerance.minimptel";

    @Produces
    @Singleton
    public OpenTelemetrySdk getOpenTelemetry(Config config) {
        return AutoConfiguredOpenTelemetrySdk.builder()
                .disableShutdownHook()
                .setServiceClassLoader(Thread.currentThread().getContextClassLoader())
                .addPropertiesSupplier(() -> {
                    Map<String, String> properties = new HashMap<>();
                    for (String propertyName : config.getPropertyNames()) {
                        if (propertyName.startsWith("otel.") || propertyName.startsWith("OTEL_")) {
                            config.getOptionalValue(propertyName, String.class)
                                    .ifPresent(value -> properties.put(propertyName, value));
                        }
                    }
                    return properties;
                })
                .build()
                .getOpenTelemetrySdk();
    }

    @Produces
    @Singleton
    public Meter getMeter(OpenTelemetrySdk sdk) {
        return sdk.getMeter(NAME);
    }

    public void close(@Disposes OpenTelemetrySdk sdk) {
        List<CompletableResultCode> shutdown = new ArrayList<>();
        shutdown.add(sdk.getSdkTracerProvider().shutdown());
        shutdown.add(sdk.getSdkMeterProvider().shutdown());
        shutdown.add(sdk.getSdkLoggerProvider().shutdown());
        CompletableResultCode.ofAll(shutdown).join(10, TimeUnit.SECONDS);
    }
}
