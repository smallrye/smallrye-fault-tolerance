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

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.smallrye.faulttolerance.minimptel.MetricsAccess;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class CircuitBreakerFailOnMetricsTest {
    @Test
    public void test(PingService pingService, MetricsAccess metrics) {
        try {
            pingService.ping();
        } catch (IllegalArgumentException | IllegalStateException expected) {
        }

        assertThat(circuitBreakerStateCurrent(metrics, "closed")).isEqualTo(1);
        assertThat(circuitBreakerStateCurrent(metrics, "open")).isEqualTo(0);
        assertThat(circuitBreakerStateCurrent(metrics, "halfOpen")).isEqualTo(0);

        for (int i = 0; i < 9; i++) {
            try {
                pingService.ping();
            } catch (IllegalArgumentException | IllegalStateException expected) {
            }
        }

        assertThat(circuitBreakerStateCurrent(metrics, "closed")).isEqualTo(0);
        assertThat(circuitBreakerStateCurrent(metrics, "open")).isEqualTo(1);
        assertThat(circuitBreakerStateCurrent(metrics, "halfOpen")).isEqualTo(0);

        assertThat(circuitBreakerCallsTotal(metrics, "success")).isEqualTo(5);
        assertThat(circuitBreakerCallsTotal(metrics, "failure")).isEqualTo(5);

        assertThat(invocationsTotal(metrics, "exceptionThrown", "notDefined")).isEqualTo(10);
    }

    // ---

    private long circuitBreakerStateCurrent(MetricsAccess metrics, String state) {
        return metrics.get(LongPointData.class, "ft.circuitbreaker.state.current",
                attributes(Attributes.of(stringKey("state"), state))).getValue();
    }

    private long circuitBreakerCallsTotal(MetricsAccess metrics, String result) {
        return metrics.get(LongPointData.class, "ft.circuitbreaker.calls.total",
                attributes(Attributes.of(stringKey("circuitBreakerResult"), result))).getValue();
    }

    private long invocationsTotal(MetricsAccess metrics, String result, String fallback) {
        return metrics.get(LongPointData.class, "ft.invocations.total",
                attributes(Attributes.of(stringKey("result"), result, stringKey("fallback"), fallback))).getValue();
    }

    private static Attributes attributes(Attributes attributes) {
        return Attributes.builder()
                .putAll(attributes)
                .put(stringKey("method"), "io.smallrye.faulttolerance.circuitbreaker.failon.metrics.PingService.ping")
                .build();
    }
}
