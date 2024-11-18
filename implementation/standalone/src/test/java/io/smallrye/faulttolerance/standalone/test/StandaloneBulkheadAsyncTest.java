package io.smallrye.faulttolerance.standalone.test;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.util.party.Party;

public class StandaloneBulkheadAsyncTest {
    @Test
    public void asyncBulkhead() throws Exception {
        TypedGuard<CompletionStage<String>> guarded = TypedGuard.create(Types.CS_STRING)
                .withBulkhead().limit(5).queueSize(5).done()
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

        assertThat(guarded.call(() -> completedFuture("value")))
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");

        party.organizer().disband();
    }

    public CompletionStage<String> fallback() {
        return completedFuture("fallback");
    }
}
