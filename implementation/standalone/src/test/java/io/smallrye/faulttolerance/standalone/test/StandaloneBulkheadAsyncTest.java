package io.smallrye.faulttolerance.standalone.test;

import static io.smallrye.faulttolerance.core.util.CompletionStages.completedStage;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.FaultTolerance;
import io.smallrye.faulttolerance.core.util.party.Party;

public class StandaloneBulkheadAsyncTest {
    @Test
    public void asyncBulkhead() throws Exception {
        FaultTolerance<CompletionStage<String>> guarded = FaultTolerance.<String> createAsync()
                .withBulkhead().limit(5).queueSize(5).done()
                .withFallback().handler(this::fallback).applyOn(BulkheadException.class).done()
                .withThreadOffload(true)
                .build();

        Party party = Party.create(5);

        for (int i = 0; i < 10; i++) {
            guarded.call(() -> {
                party.participant().attend();
                return completedStage("ignored");
            });
        }

        party.organizer().waitForAll();

        assertThat(guarded.call(() -> completedStage("value")))
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");

        party.organizer().disband();
    }

    public CompletionStage<String> fallback() {
        return completedStage("fallback");
    }
}
