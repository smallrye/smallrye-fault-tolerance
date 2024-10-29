package io.smallrye.faulttolerance.async.future.bulkhead.stress;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class FutureBulkheadStressTest {
    @Test
    public void stressTest(BulkheadService service) throws InterruptedException, ExecutionException {
        for (int i = 0; i < 50; i++) {
            service.reset();
            iteration(service);
        }
    }

    private void iteration(BulkheadService service) throws InterruptedException, ExecutionException {
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < BulkheadService.BULKHEAD_SIZE + BulkheadService.BULKHEAD_QUEUE_SIZE; i++) {
            futures.add(service.hello());
        }

        Set<String> results = new HashSet<>();
        for (Future<String> future : futures) {
            results.add(future.get());
        }

        Assertions.assertThat(results).hasSize(BulkheadService.BULKHEAD_SIZE + BulkheadService.BULKHEAD_QUEUE_SIZE);

        for (int i = 0; i < BulkheadService.BULKHEAD_SIZE + BulkheadService.BULKHEAD_QUEUE_SIZE; i++) {
            // assertThat(results).contains("" + i) is incredibly slow
            assertThat(results.contains("" + i)).isTrue();
        }
    }
}
