package io.smallrye.faulttolerance.reuse.async.uni.threadoffload.guard;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class ReuseAsyncUniThreadOffloadGuardTest {
    @Test
    public void test(MyService service) throws ExecutionException, InterruptedException {
        testCompatible(service);
    }

    @Test
    @WithSystemProperty(key = "smallrye.faulttolerance.mp-compatibility", value = "true")
    public void testCompatible(MyService service) throws ExecutionException, InterruptedException {
        Thread currentThread = Thread.currentThread();

        // not truly async per `SpecCompatibility`, so thread offload happens per the `Guard` config
        assertThat(service.hello().subscribeAsCompletionStage().get()).isEqualTo("hello");
        assertThat(MyService.CURRENT_THREAD).doesNotHaveValue(currentThread);

        assertThat(service.helloBlocking().subscribeAsCompletionStage().get()).isEqualTo("hello");
        assertThat(MyService.CURRENT_THREAD_BLOCKING).doesNotHaveValue(currentThread);

        assertThat(service.helloNonBlocking().subscribeAsCompletionStage().get()).isEqualTo("hello");
        assertThat(MyService.CURRENT_THREAD_NONBLOCKING).hasValue(currentThread);
    }

    @Test
    @WithSystemProperty(key = "smallrye.faulttolerance.mp-compatibility", value = "false")
    public void testNoncompatible(MyService service) throws ExecutionException, InterruptedException {
        Thread currentThread = Thread.currentThread();

        // truly async per `SpecCompatibility` and non-blocking by absence of annotation, no thread offload
        assertThat(service.hello().subscribeAsCompletionStage().get()).isEqualTo("hello");
        assertThat(MyService.CURRENT_THREAD).hasValue(currentThread);

        assertThat(service.helloBlocking().subscribeAsCompletionStage().get()).isEqualTo("hello");
        assertThat(MyService.CURRENT_THREAD_BLOCKING).doesNotHaveValue(currentThread);

        assertThat(service.helloNonBlocking().subscribeAsCompletionStage().get()).isEqualTo("hello");
        assertThat(MyService.CURRENT_THREAD_NONBLOCKING).hasValue(currentThread);
    }
}
