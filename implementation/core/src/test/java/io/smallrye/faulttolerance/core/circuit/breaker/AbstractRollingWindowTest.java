package io.smallrye.faulttolerance.core.circuit.breaker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public abstract class AbstractRollingWindowTest {
    @Test
    public void specScenario1() {
        RollingWindow window = createRollingWindow(4, 2);

        assertThat(window.recordSuccess()).isFalse();
        assertThat(window.recordFailure()).isFalse();
        assertThat(window.recordFailure()).isFalse();
        assertThat(window.recordSuccess()).isTrue();
    }

    @Test
    public void specScenario2() {
        RollingWindow window = createRollingWindow(4, 2);

        assertThat(window.recordSuccess()).isFalse();
        assertThat(window.recordFailure()).isFalse();
        assertThat(window.recordSuccess()).isFalse();
        assertThat(window.recordSuccess()).isFalse();
        assertThat(window.recordFailure()).isTrue();
    }

    protected abstract RollingWindow createRollingWindow(int size, int failureThreshold);
}
