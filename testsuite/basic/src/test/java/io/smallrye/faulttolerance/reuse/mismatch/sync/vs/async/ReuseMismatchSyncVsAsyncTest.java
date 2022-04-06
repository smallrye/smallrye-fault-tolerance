package io.smallrye.faulttolerance.reuse.mismatch.sync.vs.async;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
public class ReuseMismatchSyncVsAsyncTest {
    @Test
    public void test(MyService service) {
        assertThatCode(service::hello)
                .isExactlyInstanceOf(FaultToleranceException.class)
                .hasMessageContaining(
                        "Configured fault tolerance 'my-fault-tolerance' expects the operation to be synchronous, but it returns CompletionStage");
    }
}
