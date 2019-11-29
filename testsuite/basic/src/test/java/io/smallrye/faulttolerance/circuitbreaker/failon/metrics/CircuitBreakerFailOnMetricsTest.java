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

import static org.junit.Assert.assertEquals;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.smallrye.faulttolerance.TestArchive;

@RunWith(Arquillian.class)
public class CircuitBreakerFailOnMetricsTest {
    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(CircuitBreakerFailOnMetricsTest.class)
                .addPackage(CircuitBreakerFailOnMetricsTest.class.getPackage());
    }

    @Test
    public void test(PingService pingService, MetricRegistry metrics) {
        for (int i = 0; i < 10; i++) {
            try {
                pingService.ping();
            } catch (IllegalArgumentException | IllegalStateException expected) {
            }
        }

        assertEquals(5, metrics.counter(
                "ft.io.smallrye.faulttolerance.circuitbreaker.failon.metrics.PingService.ping.circuitbreaker.callsSucceeded.total")
                .getCount());
        assertEquals(5, metrics.counter(
                "ft.io.smallrye.faulttolerance.circuitbreaker.failon.metrics.PingService.ping.circuitbreaker.callsFailed.total")
                .getCount());
        assertEquals(10,
                metrics.counter(
                        "ft.io.smallrye.faulttolerance.circuitbreaker.failon.metrics.PingService.ping.invocations.failed.total")
                        .getCount());
        assertEquals(10,
                metrics.counter(
                        "ft.io.smallrye.faulttolerance.circuitbreaker.failon.metrics.PingService.ping.invocations.total")
                        .getCount());
    }
}
