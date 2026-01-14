package io.smallrye.faulttolerance.minimptel;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;

public class ExporterProviders {
    public static class LogRecords implements ConfigurableLogRecordExporterProvider {
        @Override
        public LogRecordExporter createExporter(ConfigProperties config) {
            return InMemoryLogRecordExporter.create();
        }

        @Override
        public String getName() {
            return "otlp";
        }
    }

    public static class Metrics implements ConfigurableMetricExporterProvider {
        @Override
        public MetricExporter createExporter(ConfigProperties config) {
            return InMemoryMetricExporter.create();
        }

        @Override
        public String getName() {
            return "otlp";
        }
    }
}
