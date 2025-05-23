package io.smallrye.faulttolerance.standalone.test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.util.party.Party;

public class StandaloneBulkheadAsyncEventsTest {
    @Test
    public void asyncBulkheadEvents() throws Exception {
        AtomicInteger acceptedCounter = new AtomicInteger();
        AtomicInteger rejectedCounter = new AtomicInteger();
        AtomicInteger finishedCounter = new AtomicInteger();

        TypedGuard<CompletionStage<String>> guarded = TypedGuard.create(Types.CS_STRING)
                .withBulkhead()
                .limit(5)
                .queueSize(5)
                .onAccepted(acceptedCounter::incrementAndGet)
                .onRejected(rejectedCounter::incrementAndGet)
                .onFinished(finishedCounter::incrementAndGet)
                .done()
                .withFallback().handler(this::fallback).applyOn(BulkheadException.class).done()
                .withThreadOffload(true)
                .build();

        Party party = Party.create(5);

        for (int i = 0; i < 10; i++) {
            guarded.call(() -> {
                party.participant().attend();
                return completedFuture("ignored");
            });
        }

        party.organizer().waitForAll();

        assertThat(acceptedCounter).hasValue(10);
        assertThat(rejectedCounter).hasValue(0);
        assertThat(finishedCounter).hasValue(0);

        for (int i = 0; i < 3; i++) {
            assertThat(guarded.call(() -> completedFuture("value")))
                    .succeedsWithin(10, TimeUnit.SECONDS)
                    .isEqualTo("fallback");
        }

        assertThat(acceptedCounter).hasValue(10);
        assertThat(rejectedCounter).hasValue(3);
        assertThat(finishedCounter).hasValue(0);

        party.organizer().disband();

        await().untilAsserted(() -> {
            assertThat(finishedCounter).hasValue(10);
        });
    }

    public CompletionStage<String> fallback() {
        return completedFuture("fallback");
    }
}
