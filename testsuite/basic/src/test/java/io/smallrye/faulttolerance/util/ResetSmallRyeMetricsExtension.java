package io.smallrye.faulttolerance.util;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.smallrye.metrics.MetricRegistries;

public class ResetSmallRyeMetricsExtension implements BeforeAllCallback {
    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        // Since MP FT 3.0, metrics are added to the "base" scope,  which persists across
        // application undeployments (see https://github.com/smallrye/smallrye-metrics/issues/12).
        // We drop all metric registries before tests, so that each test has its own set
        // of metric registries and there's no cross-test pollution.
        MetricRegistries.dropAll();
    }
}
