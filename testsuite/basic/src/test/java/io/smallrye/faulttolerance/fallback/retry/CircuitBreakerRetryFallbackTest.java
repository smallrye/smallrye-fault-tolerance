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
package io.smallrye.faulttolerance.fallback.retry;

import static org.junit.Assert.assertEquals;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.smallrye.faulttolerance.TestArchive;

/**
 *
 * @author Martin Kouba
 */
@RunWith(Arquillian.class)
public class CircuitBreakerRetryFallbackTest {

    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(CircuitBreakerRetryFallbackTest.class).addPackage(CircuitBreakerRetryFallbackTest.class.getPackage());
    }

    @Test
    public void testCircuitBreakerOpens(SuperCoolService coolService) throws InterruptedException {
        // Total invocations = 9 because maxRetries = 2
        int loop = 3;
        for (int i = 1; i <= loop; i++) {
            // Fallback is always used
            assertEquals(SuperCoolService.class.getName(), coolService.ping());
        }
        // After 5 invocations the circuit should be open
        assertEquals(5, coolService.getCounter().get());
    }

}