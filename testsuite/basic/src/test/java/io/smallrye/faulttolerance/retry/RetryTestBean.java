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
package io.smallrye.faulttolerance.retry;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * @author Radoslav Husar
 */
@Dependent
public class RetryTestBean {

    private AtomicInteger attempt = new AtomicInteger();

    @Retry(maxRetries = -1, retryOn = NullPointerException.class, abortOn = IllegalArgumentException.class)
    public void callWithUnlimitedRetries() {
        int attempt = this.attempt.getAndIncrement();
        if (attempt < 5) {
            throw new NullPointerException();
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Retry(maxDuration = 500L, maxRetries = 2)
    @Fallback(fallbackMethod = "fallback")
    @Timeout(500L)
    public String callWithMaxDuration500ms(Long... sleepTime) {
        return call(sleepTime);
    }

    @Retry(maxDuration = 2000L, maxRetries = 2)
    @Fallback(fallbackMethod = "fallback")
    @Timeout(500L)
    public String callWithMaxDuration2s(Long... sleepTime) {
        return call(sleepTime);
    }

    @Retry(retryOn = TimeoutException.class)
    @Fallback(fallbackMethod = "fallback")
    @Timeout(100L)
    public String callWithRetryOnTimeoutException() {
        int attempt = this.attempt.getAndIncrement();
        if (attempt == 0) {
            sleep(5000L);
        }
        return "call" + attempt;
    }

    @Retry(retryOn = OutOfMemoryError.class)
    @Fallback(fallbackMethod = "fallback")
    @Timeout(100L)
    public String callWithRetryOnOutOfMemoryError() {
        int attempt = this.attempt.getAndIncrement();
        if (attempt == 0) {
            sleep(5000L);
        }
        return "call" + attempt;
    }

    @Retry(retryOn = BulkheadException.class, delay = 500)
    @Bulkhead(2)
    @Fallback(fallbackMethod = "fallback")
    public String callWithRetryOnBulkhead() {
        int attempt = this.attempt.getAndIncrement();
        // both first attempts should take long time
        // without @Asynchronous, the third attempt should fail right away with BulkheadException and should be retried
        // the third attempt should be retried in 500 ms, after the first two calls were processed and should be successful
        // no fallback should be called
        if (attempt < 2) {
            sleep(300L);
        }
        return "call" + attempt;
    }

    @Retry(retryOn = TimeoutException.class, delay = 500)
    @Bulkhead(2)
    public String callWithNoRetryOnBulkhead() {
        int attempt = this.attempt.getAndIncrement();
        if (attempt < 2) {
            sleep(300L);
        }
        return "call" + attempt;
    }

    @Retry(retryOn = TimeoutException.class, delay = 500)
    @Bulkhead(2)
    @Fallback(fallbackMethod = "fallback")
    public String callWithFallbackAndNoRetryOnBulkhead() {
        int attempt = this.attempt.getAndIncrement();
        if (attempt < 2) {
            sleep(300L);
        }
        return "call" + attempt;
    }

    private String call(Long[] sleepTime) {
        int attempt = this.attempt.getAndIncrement();
        if (attempt < sleepTime.length) {
            sleep(sleepTime[attempt]);
        }
        return "call" + attempt;
    }

    private void sleep(Long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    @SuppressWarnings("unused")
    public String fallback(Long... ignored) {
        return fallback();
    }

    public String fallback() {
        return "fallback" + attempt;
    }

    public void reset() {
        attempt.set(0);
    }
}
