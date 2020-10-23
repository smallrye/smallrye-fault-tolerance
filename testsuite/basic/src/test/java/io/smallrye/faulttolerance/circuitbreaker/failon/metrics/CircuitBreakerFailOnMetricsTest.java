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

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class CircuitBreakerFailOnMetricsTest {
    @Test
    public void test(PingService pingService, @RegistryType(type = MetricRegistry.Type.BASE) MetricRegistry metrics) {
        for (int i = 0; i < 10; i++) {
            try {
                pingService.ping();
            } catch (IllegalArgumentException | IllegalStateException expected) {
            }
        }

        assertThat(metrics.counter("ft.circuitbreaker.calls.total",
                new Tag("method", "io.smallrye.faulttolerance.circuitbreaker.failon.metrics.PingService.ping"),
                new Tag("circuitBreakerResult", "success"))
                .getCount()).isEqualTo(5);
        assertThat(metrics.counter("ft.circuitbreaker.calls.total",
                new Tag("method", "io.smallrye.faulttolerance.circuitbreaker.failon.metrics.PingService.ping"),
                new Tag("circuitBreakerResult", "failure"))
                .getCount()).isEqualTo(5);
        assertThat(metrics.counter("ft.invocations.total",
                new Tag("method", "io.smallrye.faulttolerance.circuitbreaker.failon.metrics.PingService.ping"),
                new Tag("result", "exceptionThrown"),
                new Tag("fallback", "notDefined"))
                .getCount()).isEqualTo(10);
    }
}
