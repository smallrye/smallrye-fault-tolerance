package io.smallrye.faulttolerance.async.compstage.bulkhead.stress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.util.party.Party;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class CompletionStageBulkheadStressTest {
    @Test
    public void stressTest(BulkheadService service) throws InterruptedException, ExecutionException {
        for (int i = 0; i < 50; i++) {
            service.reset();
            iteration(service);
        }
    }

    private void iteration(BulkheadService service) throws InterruptedException, ExecutionException {
        List<CompletionStage<String>> futures = new ArrayList<>();
        for (int i = 0; i < BulkheadService.BULKHEAD_SIZE + BulkheadService.BULKHEAD_QUEUE_SIZE; i++) {
            futures.add(service.hello());
        }

        Set<String> results = new HashSet<>();
        for (CompletionStage<String> future : futures) {
            results.add(future.toCompletableFuture().get());
        }

        assertThat(results).hasSize(BulkheadService.BULKHEAD_SIZE + BulkheadService.BULKHEAD_QUEUE_SIZE);

        for (int i = 0; i < BulkheadService.BULKHEAD_SIZE + BulkheadService.BULKHEAD_QUEUE_SIZE; i++) {
            // assertThat(results).contains("" + i) is incredibly slow
            assertThat(results.contains("" + i)).isTrue();
        }
    }

    @Test
    public void stressTestWithWaiting(BulkheadService service) throws InterruptedException, ExecutionException {
        for (int i = 0; i < 50; i++) {
            service.reset();
            iterationWithWaiting(service);
        }
    }

    private void iterationWithWaiting(BulkheadService service) throws InterruptedException, ExecutionException {
        Party party = Party.create(BulkheadService.BULKHEAD_SIZE);

        List<CompletionStage<String>> futures = new ArrayList<>();
        for (int i = 0; i < BulkheadService.BULKHEAD_SIZE + BulkheadService.BULKHEAD_QUEUE_SIZE; i++) {
            futures.add(service.helloWithWaiting(party.participant()));
        }

        party.organizer().waitForAll();

        for (int i = 0; i < BulkheadService.BULKHEAD_SIZE * 2; i++) {
            assertThatThrownBy(() -> {
                service.helloWithWaiting(null).toCompletableFuture().get();
            }).isExactlyInstanceOf(ExecutionException.class).hasCauseExactlyInstanceOf(BulkheadException.class);
        }

        party.organizer().disband();

        Set<String> results = new HashSet<>();
        for (CompletionStage<String> future : futures) {
            results.add(future.toCompletableFuture().get());
        }

        assertThat(results).hasSize(BulkheadService.BULKHEAD_SIZE + BulkheadService.BULKHEAD_QUEUE_SIZE);

        for (int i = 0; i < BulkheadService.BULKHEAD_SIZE + BulkheadService.BULKHEAD_QUEUE_SIZE; i++) {
            // assertThat(results).contains("" + i) is incredibly slow
            assertThat(results.contains("" + i)).isTrue();
        }
    }
}
