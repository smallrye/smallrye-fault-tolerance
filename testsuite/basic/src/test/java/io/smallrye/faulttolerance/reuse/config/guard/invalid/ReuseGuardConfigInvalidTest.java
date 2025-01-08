package io.smallrye.faulttolerance.reuse.config.guard.invalid;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@AddBeanClasses(MyFaultTolerance.class)
@WithSystemProperty(key = "smallrye.faulttolerance.\"my-fault-tolerance\".retry.max-retries", value = "7")
public class ReuseGuardConfigInvalidTest {
    @Test
    public void test(MyService service) throws ExecutionException, InterruptedException {
        // access the guard programmatically, to force instantiation
        // later, config won't be read, because the guard already exists
        assertThat(MyFaultTolerance.GUARD.get(() -> "ignored", String.class)).isEqualTo("ignored");

        assertThat(service.hello()).isEqualTo("hello");
        assertThat(MyService.COUNTER).hasValue(3); // _not_ the configured value
    }
}
