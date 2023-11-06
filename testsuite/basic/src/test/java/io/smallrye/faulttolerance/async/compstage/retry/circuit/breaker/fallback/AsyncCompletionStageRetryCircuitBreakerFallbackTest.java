package io.smallrye.faulttolerance.async.compstage.retry.circuit.breaker.fallback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
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

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class AsyncCompletionStageRetryCircuitBreakerFallbackTest {
    @Test
    public void syncFailures(AsyncHelloService helloService)
            throws IOException, ExecutionException, InterruptedException {
        Map<String, Range> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello", new Range(0, 4));
        expectedResponses.put("Fallback", new Range(12, 16));
        test(16, expectedResponses, helloService::hello);

        // ensure circuit is opened (note number 44 does request correct behavior!)
        assertThat(helloService.hello(44).toCompletableFuture().get()).isEqualTo("Fallback44");

        // ensure circuit is closed
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(helloService.hello(44).toCompletableFuture().get()).isEqualTo("Hello44");
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
        assertThat(helloService.helloFailAsync(44).toCompletableFuture().get()).isEqualTo("Fallback44");

        // ensure circuit is closed
        await().atMost(3, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(helloService.helloFailAsync(44).toCompletableFuture().get()).isEqualTo("Hello44");
        });
    }

    private interface Invocation {
        CompletionStage<String> call(int i) throws Exception;
    }

    private static void test(int parallelRequests, Map<String, Range> expectedResponses, Invocation invocation)
            throws InterruptedException {
        Set<String> violations = ConcurrentHashMap.newKeySet();
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
        assertThat(finished).isTrue();

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
        assertThat(violations).isEmpty();
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
