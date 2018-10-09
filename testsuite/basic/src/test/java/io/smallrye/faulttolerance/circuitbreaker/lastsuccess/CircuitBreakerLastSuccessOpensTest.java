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
package io.smallrye.faulttolerance.circuitbreaker.lastsuccess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
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
public class CircuitBreakerLastSuccessOpensTest {

    static final int THRESHOLD = 4;

    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(CircuitBreakerLastSuccessOpensTest.class).addPackage(CircuitBreakerLastSuccessOpensTest.class.getPackage());
    }

    @Test
    public void testCircuitBreakerOpens(PingService pingService) throws InterruptedException {
        for (int i = 1; i <= THRESHOLD - 1; i++) {
            try {
                pingService.ping(false);
                fail();
            } catch (IllegalStateException expected) {
            }
        }
        pingService.ping(true);
        // Circuit should be open now
        try {
            pingService.ping(true);
            fail();
        } catch (CircuitBreakerOpenException expected) {
        }
        assertEquals(THRESHOLD, pingService.getPingCounter().get());
    }

    @ApplicationScoped
    static class PingService {

        private AtomicInteger pingCounter = new AtomicInteger(0);

        @CircuitBreaker(requestVolumeThreshold = THRESHOLD)
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