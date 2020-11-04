package io.smallrye.faulttolerance.async.types.mutiny;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
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
    public void nonblocking(HelloService service) {
        Uni<String> hello = service.helloNonblocking();
        Assertions.assertThat(hello.await().indefinitely()).isEqualTo("hello");
        assertThat(HelloService.COUNTER).hasValue(4);
    }

    @Test
    public void blocking(HelloService service) {
        Uni<String> hello = service.helloBlocking();
        Assertions.assertThat(hello.await().indefinitely()).isEqualTo("hello");
        assertThat(HelloService.COUNTER).hasValue(4);
    }

    @Test
    public void asynchronous(HelloService service) {
        Uni<String> hello = service.helloAsynchronous();
        Assertions.assertThat(hello.await().indefinitely()).isEqualTo("hello");
        assertThat(HelloService.COUNTER).hasValue(4);
    }

    @Test
    public void asynchronousNonblocking(HelloService service) {
        Uni<String> hello = service.helloAsynchronousNonblocking();
        Assertions.assertThat(hello.await().indefinitely()).isEqualTo("hello");
        assertThat(HelloService.COUNTER).hasValue(4);
    }

    @Test
    public void asynchronousBlocking(HelloService service) {
        Uni<String> hello = service.helloAsynchronousBlocking();
        Assertions.assertThat(hello.await().indefinitely()).isEqualTo("hello");
        assertThat(HelloService.COUNTER).hasValue(4);
    }
}
