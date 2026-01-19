package io.smallrye.faulttolerance.minimptel;

import java.util.Collection;
import java.util.List;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;

public class MetricsAccess {
    private final InMemoryMetricReader reader;

    MetricsAccess(InMemoryMetricReader reader) {
        this.reader = reader;
    }

    public <T extends PointData> T get(Class<T> clazz, String name, Attributes attributes) {
        if (reader == null) {
            return null;
        }
        for (MetricData data : reader.collectAllMetrics()) {
            if (data.getName().equals(name)) {
                for (PointData point : data.getData().getPoints()) {
                    if (point.getAttributes().equals(attributes) && clazz.isAssignableFrom(point.getClass())) {
                        return clazz.cast(point);
                    }
                }
            }
        }
        return null;
    }

    public Collection<? extends PointData> getAll(String name) {
        if (reader == null) {
            return null;
        }
        for (MetricData data : reader.collectAllMetrics()) {
            if (data.getName().equals(name)) {
                return data.getData().getPoints();
            }
        }
        return List.of();
    }
}
