package io.smallrye.faulttolerance.standalone.test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.core.util.party.Party;

public class StandaloneThreadOffloadTest {
    @Test
    public void integratedExecutor() throws Exception {
        FaultTolerance<CompletionStage<String>> guarded = FaultTolerance.<String> createAsync()
                .withThreadOffload(true)
                .build();

        Set<String> threadNames = ConcurrentHashMap.newKeySet();
        Party party = Party.create(5);

        for (int i = 0; i < 5; i++) {
            guarded.call(() -> {
                threadNames.add(Thread.currentThread().getName());
                party.participant().attend();
                return completedFuture("ignored");
            });
        }

        party.organizer().waitForAll();
        party.organizer().disband();

        assertThat(threadNames).hasSize(5);
    }

    @Test
    public void explicitExecutor() throws Exception {
        String prefix = UUID.randomUUID().toString();
        AtomicInteger counter = new AtomicInteger();
        ExecutorService executor = Executors.newCachedThreadPool(runnable -> new Thread(runnable,
                prefix + "_" + counter.incrementAndGet()));

        FaultTolerance<CompletionStage<String>> guarded = FaultTolerance.<String> createAsync()
                .withThreadOffload(true)
                .withThreadOffloadExecutor(executor)
                .build();

        Set<String> threadNames = ConcurrentHashMap.newKeySet();
        Party party = Party.create(5);

        for (int i = 0; i < 5; i++) {
            guarded.call(() -> {
                threadNames.add(Thread.currentThread().getName());
                party.participant().attend();
                return completedFuture("ignored");
            });
        }

        party.organizer().waitForAll();
        party.organizer().disband();

        assertThat(threadNames).hasSize(5);
        assertThat(threadNames).allSatisfy(threadName -> {
            assertThat(threadName).startsWith(prefix);
        });

        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }
}
