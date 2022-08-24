package io.smallrye.faulttolerance.async.types.rxjava;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Single;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class RxjavaTest {
    @BeforeEach
    public void setUp() {
        HelloService.COUNTER.set(0);
    }

    @Test
    public void nonblocking(HelloService service) {
        Single<String> hello = service.helloNonblocking();
        assertThat(hello.blockingGet()).isEqualTo("hello");
        assertThat(HelloService.COUNTER).hasValue(4);
    }

    @Test
    public void blocking(HelloService service) {
        Single<String> hello = service.helloBlocking();
        assertThat(hello.blockingGet()).isEqualTo("hello");
        assertThat(HelloService.COUNTER).hasValue(4);
    }

    @Test
    public void asynchronous(HelloService service) {
        Single<String> hello = service.helloAsynchronous();
        assertThat(hello.blockingGet()).isEqualTo("hello");
        assertThat(HelloService.COUNTER).hasValue(4);
    }

    @Test
    public void asynchronousNonBlocking(HelloService service) {
        Single<String> hello = service.helloAsynchronousNonBlocking();
        assertThat(hello.blockingGet()).isEqualTo("hello");
        assertThat(HelloService.COUNTER).hasValue(4);
    }

    @Test
    public void asynchronousNonblockingCombined(HelloService service) {
        Single<String> hello = service.helloAsynchronousNonblockingCombined();
        assertThat(hello.blockingGet()).isEqualTo("hello");
        assertThat(HelloService.COUNTER).hasValue(4);
    }

    @Test
    public void asynchronousBlockingCombined(HelloService service) {
        Single<String> hello = service.helloAsynchronousBlockingCombined();
        assertThat(hello.blockingGet()).isEqualTo("hello");
        assertThat(HelloService.COUNTER).hasValue(4);
    }
}
