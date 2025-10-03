package io.smallrye.faulttolerance.vertx.future;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceIntegrationTest;
import io.vertx.core.Future;

@FaultToleranceIntegrationTest
public class VertxFutureTest {
    @BeforeEach
    public void setUp() {
        HelloService.COUNTER.set(0);
    }

    @Test
    public void asynchronous(HelloService service) {
        Future<String> hello = service.helloAsynchronous();
        assertThat(hello.toCompletionStage().toCompletableFuture().join()).isEqualTo("hello");
        assertThat(HelloService.COUNTER).hasValue(4);
    }

    @Test
    public void asynchronousNonBlocking(HelloService service) {
        Future<String> hello = service.helloAsynchronousNonBlocking();
        assertThat(hello.toCompletionStage().toCompletableFuture().join()).isEqualTo("hello");
        assertThat(HelloService.COUNTER).hasValue(4);
    }
}
