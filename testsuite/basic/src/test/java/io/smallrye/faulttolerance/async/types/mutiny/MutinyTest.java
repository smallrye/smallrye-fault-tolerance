package io.smallrye.faulttolerance.async.types.mutiny;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.mutiny.Uni;

@FaultToleranceBasicTest
public class MutinyTest {
    @BeforeEach
    public void setUp() {
        HelloService.COUNTER.set(0);
    }

    @Test
    public void asynchronous(HelloService service) {
        Uni<String> hello = service.helloAsynchronous();
        assertThat(hello.await().indefinitely()).isEqualTo("hello");
        assertThat(HelloService.COUNTER).hasValue(4);
    }

    @Test
    public void asynchronousNonBlocking(HelloService service) {
        Uni<String> hello = service.helloAsynchronousNonBlocking();
        assertThat(hello.await().indefinitely()).isEqualTo("hello");
        assertThat(HelloService.COUNTER).hasValue(4);
    }
}
