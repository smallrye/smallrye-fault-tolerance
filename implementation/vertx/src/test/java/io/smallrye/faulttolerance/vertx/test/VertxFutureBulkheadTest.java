package io.smallrye.faulttolerance.vertx.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.util.party.Party;
import io.vertx.core.Future;

public class VertxFutureBulkheadTest {
    @Test
    public void bulkhead() throws Exception {
        TypedGuard<Future<String>> guarded = TypedGuard.create(Types.FUTURE_STRING)
                .withBulkhead().limit(5).queueSize(5).done()
                .withFallback().handler(this::fallback).applyOn(BulkheadException.class).done()
                .withThreadOffload(true)
                .build();

        Party party = Party.create(5);

        for (int i = 0; i < 10; i++) {
            guarded.call(() -> {
                party.participant().attend();
                return Future.succeededFuture("ignored");
            });
        }

        party.organizer().waitForAll();

        assertThat(guarded.call(() -> Future.succeededFuture("value")).toCompletionStage())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("fallback");

        party.organizer().disband();
    }

    public Future<String> fallback() {
        return Future.succeededFuture("fallback");
    }
}
