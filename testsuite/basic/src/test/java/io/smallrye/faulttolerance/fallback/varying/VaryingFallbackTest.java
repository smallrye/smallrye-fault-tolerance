package io.smallrye.faulttolerance.fallback.varying;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class VaryingFallbackTest {
    @Inject
    HelloService service;

    @Test
    public void sync() throws IOException {
        assertThat(service.hello(1)).isEqualTo("Hello 1");
        assertThat(service.hello(2)).isEqualTo("Hello 2");
        assertThat(service.hello(3)).isEqualTo("Hello 3");
    }

    @Test
    public void completionStageFailingSync() throws IOException, ExecutionException, InterruptedException {
        assertThat(service.helloCompletionStageFailingSync(1).toCompletableFuture().get()).isEqualTo("Hello 1");
        assertThat(service.helloCompletionStageFailingSync(2).toCompletableFuture().get()).isEqualTo("Hello 2");
        assertThat(service.helloCompletionStageFailingSync(3).toCompletableFuture().get()).isEqualTo("Hello 3");
    }

    @Test
    public void completionStageFailingAsync() throws IOException, ExecutionException, InterruptedException {
        assertThat(service.helloCompletionStageFailingAsync(1).toCompletableFuture().get()).isEqualTo("Hello 1");
        assertThat(service.helloCompletionStageFailingAsync(2).toCompletableFuture().get()).isEqualTo("Hello 2");
        assertThat(service.helloCompletionStageFailingAsync(3).toCompletableFuture().get()).isEqualTo("Hello 3");
    }

    @Test
    public void futureFailingSync() throws IOException, ExecutionException, InterruptedException {
        assertThat(service.helloFutureFailingSync(1).get()).isEqualTo("Hello 1");
        assertThat(service.helloFutureFailingSync(2).get()).isEqualTo("Hello 2");
        assertThat(service.helloFutureFailingSync(3).get()).isEqualTo("Hello 3");
    }

    @Test
    public void futureFailingAsync() {
        assertThatThrownBy(() -> {
            service.helloFutureFailingAsync(1).get();
        }).isExactlyInstanceOf(ExecutionException.class).hasCauseExactlyInstanceOf(IOException.class);

        assertThatThrownBy(() -> {
            service.helloFutureFailingAsync(2).get();
        }).isExactlyInstanceOf(ExecutionException.class).hasCauseExactlyInstanceOf(IOException.class);

        assertThatThrownBy(() -> {
            service.helloFutureFailingAsync(3).get();
        }).isExactlyInstanceOf(ExecutionException.class).hasCauseExactlyInstanceOf(IOException.class);
    }
}
