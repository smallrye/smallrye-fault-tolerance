package io.smallrye.faulttolerance.reuse.mixed.async.uni.threadoffload;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class MixedReuseAsyncUniThreadOffloadTest {
    @Test
    public void test(MyService service) throws ExecutionException, InterruptedException {
        testCompatible(service);
    }

    @Test
    @WithSystemProperty(key = "smallrye.faulttolerance.mp-compatibility", value = "true")
    public void testCompatible(MyService service) throws ExecutionException, InterruptedException {
        Thread currentThread = Thread.currentThread();

        // not truly async per `SpecCompatibility`, so thread offload happens per the `Guard` config
        assertThat(service.hello().subscribeAsCompletionStage().toCompletableFuture().get()).isEqualTo("hello");
        assertThat(MyService.CURRENT_THREAD_HELLO).doesNotHaveValue(currentThread);

        assertThat(service.helloBlocking().subscribeAsCompletionStage().toCompletableFuture().get()).isEqualTo("hello");
        assertThat(MyService.CURRENT_THREAD_HELLO_BLOCKING).doesNotHaveValue(currentThread);

        assertThat(service.helloNonBlocking().subscribeAsCompletionStage().toCompletableFuture().get()).isEqualTo("hello");
        assertThat(MyService.CURRENT_THREAD_HELLO_NONBLOCKING).hasValue(currentThread);

        // not truly async per `SpecCompatibility`, so thread offload happens per the `Guard` config
        assertThat(service.theAnswer().subscribeAsCompletionStage().toCompletableFuture().get()).isEqualTo(42);
        assertThat(MyService.CURRENT_THREAD_THEANSWER).doesNotHaveValue(currentThread);

        assertThat(service.theAnswerBlocking().subscribeAsCompletionStage().toCompletableFuture().get()).isEqualTo(42);
        assertThat(MyService.CURRENT_THREAD_THEANSWER_BLOCKING).doesNotHaveValue(currentThread);

        assertThat(service.theAnswerNonBlocking().subscribeAsCompletionStage().toCompletableFuture().get()).isEqualTo(42);
        assertThat(MyService.CURRENT_THREAD_THEANSWER_NONBLOCKING).hasValue(currentThread);
    }

    @Test
    @WithSystemProperty(key = "smallrye.faulttolerance.mp-compatibility", value = "false")
    public void testNoncompatible(MyService service) throws ExecutionException, InterruptedException {
        Thread currentThread = Thread.currentThread();

        // truly async per `SpecCompatibility` and non-blocking by absence of annotation, no thread offload
        assertThat(service.hello().subscribeAsCompletionStage().toCompletableFuture().get()).isEqualTo("hello");
        assertThat(MyService.CURRENT_THREAD_HELLO).hasValue(currentThread);

        assertThat(service.helloBlocking().subscribeAsCompletionStage().toCompletableFuture().get()).isEqualTo("hello");
        assertThat(MyService.CURRENT_THREAD_HELLO_BLOCKING).doesNotHaveValue(currentThread);

        assertThat(service.helloNonBlocking().subscribeAsCompletionStage().toCompletableFuture().get()).isEqualTo("hello");
        assertThat(MyService.CURRENT_THREAD_HELLO_NONBLOCKING).hasValue(currentThread);

        // truly async per `SpecCompatibility` and non-blocking by absence of annotation, no thread offload
        assertThat(service.theAnswer().subscribeAsCompletionStage().toCompletableFuture().get()).isEqualTo(42);
        assertThat(MyService.CURRENT_THREAD_THEANSWER).hasValue(currentThread);

        assertThat(service.theAnswerBlocking().subscribeAsCompletionStage().toCompletableFuture().get()).isEqualTo(42);
        assertThat(MyService.CURRENT_THREAD_THEANSWER_BLOCKING).doesNotHaveValue(currentThread);

        assertThat(service.theAnswerNonBlocking().subscribeAsCompletionStage().toCompletableFuture().get()).isEqualTo(42);
        assertThat(MyService.CURRENT_THREAD_THEANSWER_NONBLOCKING).hasValue(currentThread);
    }
}
