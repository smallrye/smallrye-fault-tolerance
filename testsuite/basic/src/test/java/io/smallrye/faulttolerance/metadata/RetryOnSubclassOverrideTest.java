package io.smallrye.faulttolerance.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

/**
 * See also https://github.com/smallrye/smallrye-fault-tolerance/issues/20
 */
@FaultToleranceBasicTest
public class RetryOnSubclassOverrideTest {
    @Test
    public void testRetryOverriden(HelloService helloService) {
        BaseService.COUNTER.set(0);
        assertThat(helloService.retry()).isEqualTo("ok");
        // 1 + 4 retries
        assertThat(BaseService.COUNTER.get()).isEqualTo(5);
    }
}
