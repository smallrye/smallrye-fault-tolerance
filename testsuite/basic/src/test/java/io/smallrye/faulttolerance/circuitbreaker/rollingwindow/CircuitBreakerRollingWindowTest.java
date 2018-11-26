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
public class CircuitBreakerRollingWindowTest {

    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(CircuitBreakerRollingWindowTest.class).addPackage(CircuitBreakerRollingWindowTest.class.getPackage());
    }

    @Test
    public void testRollingWindow(PingService pingService) throws InterruptedException {
        // 5 successfull requests
        for (int i = 0; i < 5; i++) {
            pingService.ping(true);
        }
        try {
            // Now fail once - this should open the circuit because the rolling window is 2
            pingService.ping(false);
        } catch (IllegalStateException expected) {
        }
        try {
            pingService.ping(true);
            fail("Circuit should be open now");
        } catch (CircuitBreakerOpenException expected) {
        }
        assertEquals(6, pingService.getPingCounter().get());

        Thread.sleep(300);

        // Now the circuit should be HALF_OPEN
        // The follwing request should close it
        pingService.ping(true);
        assertEquals(7, pingService.getPingCounter().get());

        for (int i = 0; i < 50; i++) {
            pingService.ping(true);
        }
        try {
            // Now fail once - this should open the circuit again
            pingService.ping(false);
        } catch (IllegalStateException expected) {
        }
        try {
            pingService.ping(true);
            fail("Circuit should be open now");
        } catch (CircuitBreakerOpenException expected) {
        }
        assertEquals(58, pingService.getPingCounter().get());
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