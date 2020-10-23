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
package io.smallrye.faulttolerance.circuitbreaker.rollingwindow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class CircuitBreakerRollingWindowTest {
    @Test
    public void testRollingWindow(PingService pingService) throws InterruptedException {
        // 5 successfull requests
        for (int i = 0; i < 5; i++) {
            pingService.ping(true);
        }

        // Now fail once - this should open the circuit because the rolling window is 2
        assertThatThrownBy(() -> {
            pingService.ping(false);
        }).isExactlyInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> {
            pingService.ping(true);
        }).as("Circuit breaker should be open now").isExactlyInstanceOf(CircuitBreakerOpenException.class);

        assertThat(pingService.getPingCounter().get()).isEqualTo(6);

        Thread.sleep(300);

        // Now the circuit should be HALF_OPEN
        // The follwing request should close it
        pingService.ping(true);
        assertThat(pingService.getPingCounter().get()).isEqualTo(7);

        for (int i = 0; i < 50; i++) {
            pingService.ping(true);
        }

        // Now fail once - this should open the circuit because the rolling window is 2
        assertThatThrownBy(() -> {
            pingService.ping(false);
        }).isExactlyInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> {
            pingService.ping(true);
        }).as("Circuit breaker should be open now").isExactlyInstanceOf(CircuitBreakerOpenException.class);

        assertThat(pingService.getPingCounter().get()).isEqualTo(58);
    }

    @ApplicationScoped
    static class PingService {
        private AtomicInteger pingCounter = new AtomicInteger(0);

        @CircuitBreaker(requestVolumeThreshold = 2, failureRatio = .40, delay = 300)
        public String ping(boolean success) {
            pingCounter.incrementAndGet();

            if (success) {
                return "ok";
            }
            throw new IllegalStateException();
        }

        AtomicInteger getPingCounter() {
            return pingCounter;
        }
    }
}
