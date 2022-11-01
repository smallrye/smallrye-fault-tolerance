/*
 * Copyright 2019 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.faulttolerance.circuitbreaker.failon.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class CircuitBreakerFailOnMetricsTest {
    @Test
    public void test(PingService pingService, @RegistryType(type = MetricRegistry.Type.BASE) MetricRegistry metrics) {
        try {
            pingService.ping();
        } catch (IllegalArgumentException | IllegalStateException expected) {
        }

        assertThat(getGauge(metrics, "ft.circuitbreaker.state.current", new Tag("state", "closed")))
                .isEqualTo(1);
        assertThat(getGauge(metrics, "ft.circuitbreaker.state.current", new Tag("state", "open")))
                .isEqualTo(0);
        assertThat(getGauge(metrics, "ft.circuitbreaker.state.current", new Tag("state", "halfOpen")))
                .isEqualTo(0);

        for (int i = 0; i < 9; i++) {
            try {
                pingService.ping();
            } catch (IllegalArgumentException | IllegalStateException expected) {
            }
        }

        assertThat(getGauge(metrics, "ft.circuitbreaker.state.current", new Tag("state", "closed")))
                .isEqualTo(0);
        assertThat(getGauge(metrics, "ft.circuitbreaker.state.current", new Tag("state", "open")))
                .isEqualTo(1);
        assertThat(getGauge(metrics, "ft.circuitbreaker.state.current", new Tag("state", "halfOpen")))
                .isEqualTo(0);

        assertThat(getCounter(metrics, "ft.circuitbreaker.calls.total", new Tag("circuitBreakerResult", "success")))
                .isEqualTo(5);
        assertThat(getCounter(metrics, "ft.circuitbreaker.calls.total", new Tag("circuitBreakerResult", "failure")))
                .isEqualTo(5);
        assertThat(getCounter(metrics, "ft.invocations.total", new Tag("result", "exceptionThrown"),
                new Tag("fallback", "notDefined"))).isEqualTo(10);
    }

    // ---

    private Long getGauge(MetricRegistry registry, String name, Tag... additionalTags) {
        return (Long) registry.getGauge(getMetricId(name, additionalTags)).getValue();
    }

    private Long getCounter(MetricRegistry registry, String name, Tag... additionalTags) {
        return registry.getCounter(getMetricId(name, additionalTags)).getCount();
    }

    private static MetricID getMetricId(String name, Tag[] additionalTags) {
        List<Tag> tags = new ArrayList<>();
        tags.add(new Tag("method", "io.smallrye.faulttolerance.circuitbreaker.failon.metrics.PingService.ping"));
        tags.addAll(Arrays.asList(additionalTags));
        return new MetricID(name, tags.toArray(new Tag[0]));
    }
}
