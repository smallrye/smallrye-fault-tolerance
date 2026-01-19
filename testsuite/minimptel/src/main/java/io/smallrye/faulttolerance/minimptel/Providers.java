package io.smallrye.faulttolerance.minimptel;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ConfigurableMetricReaderProvider;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class Providers {
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

    public static class Spans implements ConfigurableSpanExporterProvider {
        @Override
        public SpanExporter createExporter(ConfigProperties config) {
            return InMemorySpanExporter.create();
        }

        @Override
        public String getName() {
            return "otlp";
        }
    }

    public static class InMemoryMetrics implements ConfigurableMetricReaderProvider {
        static volatile InMemoryMetricReader reader;

        @Override
        public MetricReader createMetricReader(ConfigProperties config) {
            return reader = InMemoryMetricReader.create();
        }

        @Override
        public String getName() {
            return "in-memory";
        }
    }
}
