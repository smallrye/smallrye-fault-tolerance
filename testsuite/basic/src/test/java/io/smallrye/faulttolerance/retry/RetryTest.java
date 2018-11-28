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

import io.smallrye.faulttolerance.TestArchive;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 11/23/18
 */
@RunWith(Arquillian.class)
public class RetryTest {

    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(RetryTest.class)
                .addPackage(RetryTest.class.getPackage());
    }

    @Inject
    private RetryTestBean retryTestBean;

    @After
    public void cleanUp() {
        retryTestBean.reset();
    }

    @Test
    public void shouldFallbackOnMaxDurationExceeded() {
        String result = retryTestBean.callWithMaxDuration500ms(600L);
        assertEquals("fallback1", result);
    }

    @Test
    public void shouldNotFallbackIfTimeNotReached() {
        String result = retryTestBean.callWithMaxDuration2s(600L, 100L);
        assertEquals("call1", result);
    }

    @Test
    public void shouldRetryOnTimeoutExceptionIfSpecified() {
        String result = retryTestBean.callWithRetryOnTimeoutException();
        assertEquals("call1", result);
    }

    @Test
    public void shouldNotRetryOnTimeoutExceptionIfNotSpecified() {
        String result = retryTestBean.callWithRetryOnOutOfMemoryError();
        assertEquals("fallback1", result);
    }

    @Test
    public void shouldRetryOnBulkheadExceptionIfSpecified() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        Callable<String> call = () -> retryTestBean.callWithRetryOnBulkhead();
        List<Future<String>> futures = executorService.invokeAll(asList(call, call, call));
        List<String> results = collectResultsAsummingFailures(futures, 0);

        assertEquals("There were " + results.size() + " results instead of 3", 3, results.size());
        assertTrue("first call failed and was expected to be successful", results.contains("call0"));
        assertTrue("second call failed and was expected to be successful", results.contains("call1"));
        assertTrue("third call failed and wasn't successfully reattempted", results.contains("call2"));
    }

    @Test
    public void shouldNotRetryOnBulkheadExceptionIfNotSpecified() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        Callable<String> call = () -> retryTestBean.callWithNoRetryOnBulkhead();
        List<Future<String>> futures = executorService.invokeAll(asList(call, call, call));

        List<String> results = collectResultsAsummingFailures(futures, 1);
        assertEquals(2, results.size());
        assertTrue("first call failed and was expected to be successful", results.contains("call0"));
        assertTrue("second call failed and was expected to be successful", results.contains("call1"));
    }
    @Test
    public void shouldFallbackOnNoRetryOnBulkhead() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        Callable<String> call = () -> retryTestBean.callWithFallbackAndNoRetryOnBulkhead();
        List<Future<String>> futures = executorService.invokeAll(asList(call, call, call));

        List<String> results = collectResultsAsummingFailures(futures, 0);
        assertEquals(3, results.size());
        assertTrue("first call failed and was expected to be successful", results.contains("call0"));
        assertTrue("second call failed and was expected to be successful", results.contains("call1"));
        assertTrue("third call didn't fall back",
                results.stream().anyMatch(t -> t.startsWith("fallback"))
        );
    }

    private List<String> collectResultsAsummingFailures(List<Future<String>> futures, int expectedFailureCount) {
        int failureCount = 0;
        List<String> resultList = new ArrayList<>();
        for (Future<String> future : futures) {
            try {
                resultList.add(future.get());
            } catch (InterruptedException | ExecutionException e) {
                failureCount ++;
            }
        }

        assertEquals("Expected " + expectedFailureCount + " failures and got: " + failureCount, expectedFailureCount, failureCount);
        return resultList;
    }
}
