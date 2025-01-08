package io.smallrye.faulttolerance.reuse.config.guard.timeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.data.Offset.offset;

import java.util.concurrent.CompletionException;

import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".timeout.value", value = "1")
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".timeout.unit", value = "seconds")
public class ReuseGuardConfigTimeoutTest {
    @Test
    public void test(MyService service) throws InterruptedException {
        long start = System.currentTimeMillis();
        assertThatThrownBy(service::hello).isExactlyInstanceOf(TimeoutException.class);
        long end = System.currentTimeMillis();
        assertThat(end - start).isCloseTo(1000L, offset(2000L));

        start = System.currentTimeMillis();
        assertThatThrownBy(service.helloCompletionStage().toCompletableFuture()::join)
                .isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(TimeoutException.class);
        end = System.currentTimeMillis();
        assertThat(end - start).isCloseTo(1000L, offset(2000L));

        start = System.currentTimeMillis();
        assertThatThrownBy(service.helloUni().subscribeAsCompletionStage()::join)
                .isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(TimeoutException.class);
        end = System.currentTimeMillis();
        assertThat(end - start).isCloseTo(1000L, offset(2000L));
    }
}
