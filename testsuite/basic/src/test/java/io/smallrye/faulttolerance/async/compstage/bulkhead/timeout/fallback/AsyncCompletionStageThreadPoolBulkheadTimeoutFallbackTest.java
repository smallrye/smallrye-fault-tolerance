package io.smallrye.faulttolerance.async.compstage.bulkhead.timeout.fallback;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class AsyncCompletionStageThreadPoolBulkheadTimeoutFallbackTest {
    @Test
    public void test(AsyncHelloService helloService) throws InterruptedException {
        Map<String, Integer> expectedResponses = new HashMap<>();
        expectedResponses.put("Hello from @Bulkhead @Timeout method", 0);
        expectedResponses.put("Fallback Hello", 50);
        test(50, expectedResponses, () -> helloService.bulkheadTimeout(true));
    }

    private interface Invocation {
        CompletionStage<String> call() throws Exception;
    }

    private static void test(int parallelRequests, Map<String, Integer> expectedResponses, Invocation invocation)
            throws InterruptedException {

        Set<String> violations = ConcurrentHashMap.newKeySet();
        Queue<String> seenResponses = new ConcurrentLinkedQueue<>();

        ExecutorService executor = Executors.newFixedThreadPool(parallelRequests);
        for (int i = 0; i < parallelRequests; i++) {
            executor.submit(() -> {
                try {
                    seenResponses.add(invocation.call().toCompletableFuture().get());
                } catch (Exception e) {
                    violations.add("Unexpected exception: " + e);
                }
            });
        }
        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        for (String seenResponse : seenResponses) {
            if (!expectedResponses.containsKey(seenResponse)) {
                violations.add("Unexpected response: " + seenResponse);
            }
        }
        for (Map.Entry<String, Integer> expectedResponse : expectedResponses.entrySet()) {
            int count = 0;
            for (String seenResponse : seenResponses) {
                if (expectedResponse.getKey().equals(seenResponse)) {
                    count++;
                }
            }
            if (count != expectedResponse.getValue()) {
                violations.add("Expected to see " + expectedResponse.getValue() + " occurrence(s) but seen " + count
                        + ": " + expectedResponse.getKey());
            }
        }
        assertThat(violations).isEmpty();
    }
}
