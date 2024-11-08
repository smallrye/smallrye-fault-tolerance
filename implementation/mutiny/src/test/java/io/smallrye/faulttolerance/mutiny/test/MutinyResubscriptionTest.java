package io.smallrye.faulttolerance.mutiny.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.core.util.TestException;
import io.smallrye.mutiny.Uni;

public class MutinyResubscriptionTest {
    private final AtomicInteger counter = new AtomicInteger();

    @BeforeEach
    public void setUp() {
        counter.set(0);
    }

    @Test
    public void doubleRetry() {
        // this test verifies resubscription, which is triggered via retry

        Supplier<Uni<String>> guarded = TypedGuard.create(Types.UNI_STRING)
                .withRetry().maxRetries(3).done()
                .build()
                .adaptSupplier(this::action);

        Uni<String> hello = guarded.get()
                .onFailure().retry().atMost(2)
                .onFailure().recoverWithItem("hello");

        assertThat(hello.subscribeAsCompletionStage())
                .succeedsWithin(10, TimeUnit.SECONDS)
                .isEqualTo("hello");

        // the MutinyFaultTolerance guard has maxRetries of 3, so 1 initial attempt + 3 retries = 4 total
        // the onFailure().retry() handler does 1 initial attempt + 2 retries = 3 total
        assertThat(counter).hasValue(4 * 3);

    }

    public Uni<String> action() {
        counter.incrementAndGet();
        return Uni.createFrom().failure(new TestException());
    }
}
