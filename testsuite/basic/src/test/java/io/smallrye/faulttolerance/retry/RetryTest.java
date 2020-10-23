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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class RetryTest {
    @Inject
    private RetryTestBean retryTestBean;

    @AfterEach
    public void cleanUp() {
        retryTestBean.reset();
    }

    @Test
    public void shouldRetryIndefinitely() {
        assertThatThrownBy(() -> {
            retryTestBean.callWithUnlimitedRetries();
        }).isExactlyInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldFallbackOnMaxDurationExceeded() {
        assertThat(retryTestBean.callWithMaxDuration500ms(600L)).isEqualTo("fallback1");
    }

    @Test
    public void shouldNotFallbackIfTimeNotReached() {
        assertThat(retryTestBean.callWithMaxDuration2s(600L, 100L)).isEqualTo("call1");
    }

    @Test
    public void shouldRetryOnTimeoutExceptionIfSpecified() {
        assertThat(retryTestBean.callWithRetryOnTimeoutException()).isEqualTo("call1");
    }

    @Test
    public void shouldNotRetryOnTimeoutExceptionIfNotSpecified() {
        assertThat(retryTestBean.callWithRetryOnOutOfMemoryError()).isEqualTo("fallback1");
    }

    @Test
    public void shouldRetryOnBulkheadExceptionIfSpecified() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        try {
            Callable<String> call = () -> retryTestBean.callWithRetryOnBulkhead();
            List<Future<String>> futures = executorService.invokeAll(asList(call, call, call));
            List<String> results = collectResultsAssumingFailures(futures, 0);

            assertThat(results).hasSize(3);
            assertThat(results).as("first call failed and was expected to be successful").contains("call0");
            assertThat(results).as("second call failed and was expected to be successful").contains("call1");
            assertThat(results).as("third call failed and wasn't successfully reattempted").contains("call2");
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    public void shouldNotRetryOnBulkheadExceptionIfNotSpecified() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        try {
            Callable<String> call = () -> retryTestBean.callWithNoRetryOnBulkhead();
            List<Future<String>> futures = executorService.invokeAll(asList(call, call, call));

            List<String> results = collectResultsAssumingFailures(futures, 1);
            assertThat(results).hasSize(2);
            assertThat(results).as("first call failed and was expected to be successful").contains("call0");
            assertThat(results).as("second call failed and was expected to be successful").contains("call1");
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    public void shouldFallbackOnNoRetryOnBulkhead() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        try {
            Callable<String> call = () -> retryTestBean.callWithFallbackAndNoRetryOnBulkhead();
            List<Future<String>> futures = executorService.invokeAll(asList(call, call, call));

            List<String> results = collectResultsAssumingFailures(futures, 0);
            assertThat(results).hasSize(3);
            assertThat(results).as("first call failed and was expected to be successful").contains("call0");
            assertThat(results).as("second call failed and was expected to be successful").contains("call1");
            assertThat(results).as("third call didn't fall back").anyMatch(t -> t.startsWith("fallback"));
        } finally {
            executorService.shutdownNow();
        }
    }

    private List<String> collectResultsAssumingFailures(List<Future<String>> futures, int expectedFailureCount) {
        int failureCount = 0;
        List<String> resultList = new ArrayList<>();
        for (Future<String> future : futures) {
            try {
                resultList.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                failureCount++;
            }
        }

        assertThat(failureCount)
                .as("Expected " + expectedFailureCount + " failures and got: " + failureCount)
                .isEqualTo(expectedFailureCount);
        return resultList;
    }
}
