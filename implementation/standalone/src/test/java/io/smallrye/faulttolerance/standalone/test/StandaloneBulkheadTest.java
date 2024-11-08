package io.smallrye.faulttolerance.standalone.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.util.party.Party;

public class StandaloneBulkheadTest {
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
    public void bulkhead() throws Exception {
        TypedGuard<String> guarded = TypedGuard.create(String.class)
                .withBulkhead().limit(5).done()
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

        assertThat(guarded.call(() -> "value")).isEqualTo("fallback");

        party.organizer().disband();
    }

    public String fallback() {
        return "fallback";
    }
}
