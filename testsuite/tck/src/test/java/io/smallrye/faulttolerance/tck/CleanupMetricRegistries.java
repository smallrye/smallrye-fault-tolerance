package io.smallrye.faulttolerance.tck;

import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.test.spi.event.suite.BeforeClass;

import io.smallrye.metrics.MetricRegistries;

public class CleanupMetricRegistries {
    public void beforeEachTestClass(@Observes BeforeClass event) {
        // TODO
        //  In MP FT 2.1, metrics are added to the "application" scope, which is automatically dropped
        //  by SmallRye Metrics when application is undeployed. Since MP FT 3.0, metrics are added to the "base" scope,
        //  which persists across application undeployments (see https://github.com/smallrye/smallrye-metrics/issues/12).
        //  However, MP FT TCK expects that this isn't the case. Specifically, AllMetricsTest and MetricsDisabledTest
        //  both use the same bean, AllMetricsBean, so if AllMetricsTest runs first, some histograms are created,
        //  and then MetricsDisabledTest fails, because those histograms are not expected to exist. Here, we drop all
        //  metric registries before each test class, to work around that.

        MetricRegistries.dropAll();
    }
}
