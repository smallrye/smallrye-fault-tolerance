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
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
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
    public void test(PingService pingService, @RegistryType(type = MetricRegistry.Type.BASE) MetricRegistry metrics) {
        for (int i = 0; i < 10; i++) {
            try {
                pingService.ping();
            } catch (IllegalArgumentException | IllegalStateException expected) {
            }
        }

        assertEquals(5, metrics.counter("ft.circuitbreaker.calls.total",
                new Tag("method", "io.smallrye.faulttolerance.circuitbreaker.failon.metrics.PingService.ping"),
                new Tag("circuitBreakerResult", "success"))
                .getCount());
        assertEquals(5, metrics.counter("ft.circuitbreaker.calls.total",
                new Tag("method", "io.smallrye.faulttolerance.circuitbreaker.failon.metrics.PingService.ping"),
                new Tag("circuitBreakerResult", "failure"))
                .getCount());
        assertEquals(10, metrics.counter("ft.invocations.total",
                new Tag("method", "io.smallrye.faulttolerance.circuitbreaker.failon.metrics.PingService.ping"),
                new Tag("result", "exceptionThrown"),
                new Tag("fallback", "notDefined"))
                .getCount());
    }
}
