package io.smallrye.faulttolerance.reuse.config.guard.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletionException;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.api.RateLimitException;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".rate-limit.value", value = "3")
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".rate-limit.window", value = "2")
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".rate-limit.window-unit", value = "seconds")
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".rate-limit.type", value = "fixed")
public class ReuseGuardConfigRateLimitTest {
    @Test
    public void test(MyService service) throws InterruptedException {
        assertThat(service.hello()).isEqualTo("hello");
        assertThat(service.helloCompletionStage().toCompletableFuture().join()).isEqualTo("hello");
        assertThat(service.helloUni().subscribeAsCompletionStage().join()).isEqualTo("hello");

        assertThatThrownBy(service::hello).isExactlyInstanceOf(RateLimitException.class);
        assertThatThrownBy(service.helloCompletionStage().toCompletableFuture()::join)
                .isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(RateLimitException.class);
        assertThatThrownBy(service.helloUni().subscribeAsCompletionStage()::join)
                .isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(RateLimitException.class);

        Thread.sleep(3000);

        assertThat(service.hello()).isEqualTo("hello");
        assertThat(service.helloCompletionStage().toCompletableFuture().join()).isEqualTo("hello");
        assertThat(service.helloUni().subscribeAsCompletionStage().join()).isEqualTo("hello");

        assertThatThrownBy(service::hello).isExactlyInstanceOf(RateLimitException.class);
        assertThatThrownBy(service.helloCompletionStage().toCompletableFuture()::join)
                .isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(RateLimitException.class);
        assertThatThrownBy(service.helloUni().subscribeAsCompletionStage()::join)
                .isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(RateLimitException.class);
    }
}
