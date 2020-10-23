/*
 * Copyright 2017 Red Hat, Inc, and individual contributors.
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

package io.smallrye.faulttolerance.basic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(SharedFallback.class) // not discovered automatically, because it's only injected non-contextually
public class CommandInterceptorTest {
    @Inject
    MyMicroservice service;

    @Inject
    MyRetryMicroservice serviceRetry;

    @Test
    public void shouldRunWithLongExecutionTime() {
        assertThat(service.sayHello()).isEqualTo(MyMicroservice.HELLO);
    }

    @Test
    public void testTimeoutFallback() {
        MyFallbackHandler.reset();
        assertThat(service.sayHelloWithFallback()).isEqualTo(MyFallbackHandler.FALLBACK);
        assertThat(MyFallbackHandler.DISPOSED).isTrue();
    }

    @Test
    public void testHelloAsync() throws InterruptedException, ExecutionException {
        Future<String> future = service.sayHelloAsync();
        assertThat(future.get()).isEqualTo(MyMicroservice.HELLO);
    }

    @Test
    public void testHelloAsyncTimeoutFallback() throws InterruptedException, ExecutionException {
        Future<String> future = service.sayHelloAsyncTimeoutFallback();
        assertThat(future.get()).isEqualTo(MyFallbackHandler.FALLBACK);
    }

    @Test
    public void testSayHelloBreaker() {
        for (int n = 0; n < 7; n++) {
            try {
                service.sayHelloBreaker();
            } catch (Exception ignored) {
            }
        }

        int count = service.getSayHelloBreakerCount();
        assertThat(count).as("The number of executions should be 4").isEqualTo(4);

    }

    @Test
    public void testSayHelloBreakerClassLevel() {
        for (int n = 0; n < 7; n++) {
            try {
                service.sayHelloBreakerClassLevel();
            } catch (Exception ignored) {
            }
        }

        int count = service.getSayHelloBreakerCount3();
        assertThat(count).as("The number of executions should be 4").isEqualTo(4);
    }

    @Test
    public void testSayHelloBreaker2() {
        for (int i = 1; i < 12; i++) {
            try {
                String result = service.sayHelloBreaker2();

                if (i < 5 || (i > 6 && i < 12)) {
                    fail("serviceA should throw an Exception in testCircuitDefaultSuccessThreshold on iteration " + i);
                }
            } catch (CircuitBreakerOpenException cboe) {
                // Expected on execution 5 and iteration 10

                if (i < 5) {
                    fail("serviceA should throw a RuntimeException in testCircuitDefaultSuccessThreshold on iteration " + i);
                } else if (i == 5) {
                    // Pause to allow the circuit breaker to half-open
                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException ignored) {
                    }
                }
            } catch (RuntimeException ex) {
                // Expected
            } catch (Exception ex) {
                // Not Expected
                fail("serviceA should throw a RuntimeException or CircuitBreakerOpenException in testCircuitDefaultSuccessThreshold "
                        + "on iteration " + i);
            }
        }

        int count = service.getSayHelloBreakerCount2();
        assertThat(count).as("The number of serviceA executions should be 9").isEqualTo(9);
    }

    @Test
    public void testClassLevelCircuitOverride() {
        for (int i = 0; i < 7; i++) {
            try {
                service.sayHelloBreakerOverride();

                if (i < 2) {
                    fail("sayHelloBreakerOverride should throw an Exception in testClassLevelCircuitOverride on iteration "
                            + i);
                }
            } catch (CircuitBreakerOpenException cboe) {
                // Expected on iteration 4
                if (i < 2) {
                    fail("sayHelloBreakerOverride should throw a RuntimeException in testClassLevelCircuitOverride on iteration "
                            + i);
                }
            } catch (RuntimeException ex) {
                // Expected
            } catch (Exception ex) {
                // Not Expected
                fail("sayHelloBreakerOverride should throw a RuntimeException or CircuitBreakerOpenException in testClassLevelCircuitOverride "
                        + "on iteration " + i);
            }
        }

        int count = service.getSayHelloBreakerCount4();
        assertThat(count).as("The number of executions should be 2").isEqualTo(2);
    }

    // @Test(enabled = true, description = "Still trying to figure out @CircuitBreaker(successThreshold=...)")
    @Test
    public void testCircuitHighSuccessThreshold() {
        for (int i = 1; i < 10; i++) {
            try {
                service.sayHelloBreakerHighThreshold();

                if (i < 5 || i > 7) {
                    fail("serviceA should throw an Exception in testCircuitHighSuccessThreshold on iteration " + i);
                }
            } catch (CircuitBreakerOpenException cboe) {
                // Expected on iteration 4 and iteration 10
                if (i < 5) {
                    fail("serviceA should throw a RuntimeException in testCircuitHighSuccessThreshold on iteration " + i);
                } else if (i == 5) {
                    // Pause to allow the circuit breaker to half-open
                    try {
                        Thread.sleep(400);
                    } catch (InterruptedException ignored) {
                    }
                }
            } catch (RuntimeException ex) {
                // Expected
            } catch (Exception ex) {
                // Not Expected
                fail("serviceA should throw a RuntimeException or CircuitBreakerOpenException in testCircuitHighSuccessThreshold "
                        + "on iteration " + i);
            }
        }

        int count = service.getSayHelloBreakerCount5();
        assertThat(count).as("The number of serviceA executions should be 7").isEqualTo(7);
    }

    /**
     * A test to exercise Circuit Breaker thresholds with sufficient retries to open the Circuit and result in a
     * CircuitBreakerOpenException.
     */
    @Test
    public void testCircuitOpenWithMoreRetries() {
        int invokeCounter = 0;
        try {
            serviceRetry.sayHelloRetry();
            invokeCounter = serviceRetry.getSayHelloRetry();
            if (invokeCounter < 4) {
                fail("serviceA should retry in testCircuitOpenWithMoreRetries on iteration " + invokeCounter);
            }
        } catch (CircuitBreakerOpenException cboe) {
            // Expected on iteration 4
            invokeCounter = serviceRetry.getSayHelloRetry();
            if (invokeCounter < 4) {
                fail("serviceA should retry in testCircuitOpenWithMoreRetries on iteration " + invokeCounter);
            }
        } catch (Exception ex) {
            // Not Expected
            invokeCounter = serviceRetry.getSayHelloRetry();
            fail("serviceA should retry or throw a CircuitBreakerOpenException in testCircuitOpenWithMoreRetries on iteration "
                    + invokeCounter);
        }

        invokeCounter = serviceRetry.getSayHelloRetry();
        assertThat(invokeCounter).as("The number of executions should be 4").isEqualTo(4);
    }

    @Test
    public void testRetryTimeout() {
        try {
            serviceRetry.serviceA(1000);
        } catch (TimeoutException ex) {
            // Expected
        } catch (RuntimeException ex) {
            // Not Expected
            fail("serviceA should not throw a RuntimeException in testRetryTimeout");
        }

        assertThat(serviceRetry.getCounterForInvokingServiceA())
                .as("The execution count should be 2 (1 retry + 1)")
                .isEqualTo(2);
    }

    @Test
    public void testFallbackSuccess() {

        try {
            String result = serviceRetry.serviceA();
            assertThat(result).as("The message should be \"fallback for serviceA\"").contains("serviceA");
        } catch (RuntimeException ex) {
            fail("serviceA should not throw a RuntimeException in testFallbackSuccess");
        }
        assertThat(serviceRetry.getCounterForInvokingServiceA())
                .as("The execution count should be 2 (1 retry + 1)")
                .isEqualTo(2);
        try {
            String result = serviceRetry.serviceB();
            assertThat(result).as("The message should be \"fallback for serviceB\"").contains("serviceB");
        } catch (RuntimeException ex) {
            fail("serviceB should not throw a RuntimeException in testFallbackSuccess");
        }
        assertThat(serviceRetry.getCounterForInvokingServiceB())
                .as("The execution count should be 3 (2 retries + 1)")
                .isEqualTo(3);
    }
}
