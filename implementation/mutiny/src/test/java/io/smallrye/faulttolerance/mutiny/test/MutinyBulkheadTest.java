package io.smallrye.faulttolerance.mutiny.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.util.party.Party;
import io.smallrye.mutiny.Uni;

public class MutinyBulkheadTest {
    @Test
    public void bulkhead() throws Exception {
        TypedGuard<Uni<String>> guarded = TypedGuard.create(Types.UNI_STRING)
                .withBulkhead().limit(5).queueSize(5).done()
                .withFallback().handler(this::fallback).applyOn(BulkheadException.class).done()
                .withThreadOffload(true)
                .build();

        Party party = Party.create(5);

        for (int i = 0; i < 10; i++) {
            guarded.call(() -> {
                party.participant().attend();
                return Uni.createFrom().item("ignored");
            }).subscribeAsCompletionStage();
        }

        party.organizer().waitForAll();

        assertThat(guarded.call(() -> Uni.createFrom().item("value")).subscribeAsCompletionStage())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");

        party.organizer().disband();
    }

    public Uni<String> fallback() {
        return Uni.createFrom().item("fallback");
    }
}
