/*
 * Copyright 2018 Red Hat, Inc, and individual contributors.
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
package io.smallrye.faulttolerance.fallback;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class CircuitBreakerWithFallbackTest {
    @Test
    public void testCircuitBreakerOpens(PingService pingService) {
        int loop = 8;
        for (int i = 1; i <= loop; i++) {
            // Fallback is always used
            assertThat(pingService.ping()).isEqualTo(PingService.class.getName());
        }

        // After 5 invocations the circuit breaker should be open
        assertThat(pingService.getPingCounter().get()).isEqualTo(5);
    }
}
