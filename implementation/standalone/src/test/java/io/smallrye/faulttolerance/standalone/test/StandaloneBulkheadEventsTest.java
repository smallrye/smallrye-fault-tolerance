package io.smallrye.faulttolerance.standalone.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.util.party.Party;

public class StandaloneBulkheadEventsTest {
    private ExecutorService executor;

    @BeforeEach
    public void setUp() {
        executor = Executors.newFixedThreadPool(5);
    }

    @AfterEach
    public void tearDown() throws InterruptedException {
        executor.shutdownNow();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    public void bulkheadEvents() throws Exception {
        AtomicInteger acceptedCounter = new AtomicInteger();
        AtomicInteger rejectedCounter = new AtomicInteger();
        AtomicInteger finishedCounter = new AtomicInteger();

        TypedGuard<String> guarded = TypedGuard.create(String.class)
                .withBulkhead()
                .limit(5)
                .onAccepted(acceptedCounter::incrementAndGet)
                .onRejected(rejectedCounter::incrementAndGet)
                .onFinished(finishedCounter::incrementAndGet)
                .done()
                .withFallback().handler(this::fallback).applyOn(BulkheadException.class).done()
                .build();

        Party party = Party.create(5);

        for (int i = 0; i < 5; i++) {
            executor.submit(() -> {
                return guarded.call(() -> {
                    party.participant().attend();
                    return "ignored";
                });
            });
        }

        party.organizer().waitForAll();

        assertThat(acceptedCounter).hasValue(5);
        assertThat(rejectedCounter).hasValue(0);
        assertThat(finishedCounter).hasValue(0);

        for (int i = 0; i < 3; i++) {
            assertThat(guarded.call(() -> "value")).isEqualTo("fallback");
        }

        assertThat(acceptedCounter).hasValue(5);
        assertThat(rejectedCounter).hasValue(3);
        assertThat(finishedCounter).hasValue(0);

        party.organizer().disband();

        await().untilAsserted(() -> {
            assertThat(finishedCounter).hasValue(5);
        });
    }

    public String fallback() {
        return "fallback";
    }
}
