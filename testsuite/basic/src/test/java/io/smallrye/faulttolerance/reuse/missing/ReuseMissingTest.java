package io.smallrye.faulttolerance.reuse.missing;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class ReuseMissingTest {
    @Test
    public void test(MyService service) {
        assertThatCode(service::hello)
                .isExactlyInstanceOf(FaultToleranceException.class)
                .hasMessageContaining("Can't resolve a bean of type")
                .hasMessageContaining("with qualifier @io.smallrye.common.annotation.Identifier(\"my-fault-tolerance\")");
    }
}
