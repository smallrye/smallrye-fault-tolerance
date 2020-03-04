package io.smallrye.faulttolerance.async.compstage.retry.circuit.breaker.fallback;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.smallrye.faulttolerance.TestArchive;

@RunWith(Arquillian.class)
public class AsyncCompletionStageRetryCircuitBreakerFallbackTest {
    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(AsyncCompletionStageRetryCircuitBreakerFallbackTest.class)
                .addPackage(AsyncCompletionStageRetryCircuitBreakerFallbackTest.class.getPackage());
    }

    @Test
    public void syncFailures(AsyncHelloService helloService)
            throws IOException, ExecutionException, InterruptedException {
        Map<String, Range> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello", new Range(0, 4));
        expectedResponses.put("Fallback", new Range(12, 16));
        test(16, expectedResponses, helloService::hello);

        // ensure circuit is opened (note number 44 does request correct behavior!)
        assertEquals("Fallback44", helloService.hello(44).toCompletableFuture().get());

        // ensure circuit is closed
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals("Hello44",
                    helloService.hello(44).toCompletableFuture().get());
        });
    }

    @Test
    public void asyncFailures(AsyncHelloService helloService)
            throws ExecutionException, InterruptedException {
        Map<String, Range> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello", new Range(0, 4));
        expectedResponses.put("Fallback", new Range(12, 16));
        test(16, expectedResponses, helloService::helloFailAsync);

        // ensure circuit is opened (note number 44 does request correct behavior!)
        assertEquals("Fallback44", helloService.helloFailAsync(44).toCompletableFuture().get());

        // ensure circuit is closed
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            assertEquals("Hello44",
                    helloService.helloFailAsync(44).toCompletableFuture().get());
        });
    }

    private interface Invocation {
        CompletionStage<String> call(int i) throws Exception;
    }

    private static void test(int parallelRequests, Map<String, Range> expectedResponses, Invocation invocation)
            throws InterruptedException {
        Set<String> violations = Collections.newSetFromMap(new ConcurrentHashMap<>());
        Queue<String> seenResponses = new ConcurrentLinkedQueue<>();

        ExecutorService executor = Executors.newFixedThreadPool(parallelRequests);
        for (int i = 0; i < parallelRequests; i++) {
            final int finalI = i;
            executor.submit(() -> {
                try {
                    String response = invocation.call(finalI).toCompletableFuture().get();
                    seenResponses.add(response);
                } catch (Exception e) {
                    violations.add("Unexpected exception: " + e);
                }
            });
        }
        executor.shutdown();
        boolean finished = executor.awaitTermination(15, TimeUnit.SECONDS);
        assertTrue(finished);

        for (String seenResponse : seenResponses) {
            if (!expectedResponses.containsKey(seenResponse.replaceAll("[0-9]", ""))) {
                violations.add("Unexpected response: " + seenResponse);
            }
        }
        for (Map.Entry<String, Range> expectedResponse : expectedResponses.entrySet()) {
            int count = 0;
            for (String seenResponse : seenResponses) {
                if (expectedResponse.getKey().equals(seenResponse.replaceAll("[0-9]", ""))) {
                    count++;
                }
            }
            if (!expectedResponse.getValue().contains(count)) {
                violations.add("Expected to see " + expectedResponse.getValue() + " occurrence(s) but seen " + count
                        + ": " + expectedResponse.getKey());
            }
        }
        if (!violations.isEmpty()) {
            fail(violations.toString());
        }
    }

    private static class Range {
        private final int lowerBound; // inclusive
        private final int higherBound; // inclusive

        public Range(int lowerBound, int higherBound) {
            this.lowerBound = lowerBound;
            this.higherBound = higherBound;
        }

        public boolean contains(int number) {
            return lowerBound <= number && number <= higherBound;
        }

        @Override
        public String toString() {
            return "" + lowerBound + "-" + higherBound;
        }
    }
}
