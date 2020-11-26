package io.smallrye.faulttolerance.async.sizing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.util.party.Party;

public abstract class AbstractAsyncThreadPoolSizingTest {
    static final int SIZE = 10;

    @Test
    public void testAsyncThreadPoolSizing(HelloService helloService) throws InterruptedException {
        Party party = Party.create(SIZE);

        List<CompletionStage<String>> futures = new ArrayList<>();

        for (int i = 0; i < 2 * SIZE; i++) {
            CompletionStage<String> future = helloService.hello(party.participant());
            futures.add(future);
        }

        party.organizer().waitForAll();
        party.organizer().disband();

        int ok = 0;
        int error = 0;
        for (CompletionStage<String> future : futures) {
            try {
                assertThat(future.toCompletableFuture().get()).isEqualTo("hello");
                ok++;
            } catch (ExecutionException e) {
                assertThat(e).hasCauseExactlyInstanceOf(RejectedExecutionException.class);
                error++;
            }
        }

        assertThat(ok).isEqualTo(SIZE);
        assertThat(error).isEqualTo(SIZE);
    }
}
