package io.smallrye.faulttolerance.async.noncompatible;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetSystemProperty;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class NoncompatibleNonblockingAsyncTest {
    @Test
    @SetSystemProperty(key = "smallrye.faulttolerance.mp-compatibility", value = "false")
    public void noncompatibleMode(NoncompatibleNonblockingHelloService service) throws Exception {
        Thread mainThread = Thread.currentThread();

        CompletionStage<String> future = service.hello();
        assertThat(future.toCompletableFuture().get()).isEqualTo("hello");

        assertThat(service.getHelloThread()).isSameAs(mainThread);
        assertThat(service.getHelloStackTrace()).anySatisfy(frame -> {
            assertThat(frame.getClassName()).contains("io.smallrye.faulttolerance.core");
        });

        assertThat(service.getFallbackThread()).isSameAs(mainThread);
    }

    @Test
    @SetSystemProperty(key = "smallrye.faulttolerance.mp-compatibility", value = "true")
    public void explicitCompatibleMode(NoncompatibleNonblockingHelloService service) {
        CompletionStage<String> future = service.hello();
        assertThatThrownBy(() -> future.toCompletableFuture().get())
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void compatibleModeByDefault(NoncompatibleNonblockingHelloService service) {
        explicitCompatibleMode(service);
    }
}
