package io.smallrye.faulttolerance.reuse.mismatch.async.vs.async;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.concurrent.ExecutionException;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class ReuseMismatchAsyncVsAsyncTest {
    @Test
    public void test(MyService service) throws ExecutionException, InterruptedException {
        assertThatCode(service::hello)
                .isExactlyInstanceOf(FaultToleranceException.class)
                .hasMessageContaining(
                        "Configured fault tolerance 'my-fault-tolerance' expects the operation to return Uni, but it returns CompletionStage");
    }
}
