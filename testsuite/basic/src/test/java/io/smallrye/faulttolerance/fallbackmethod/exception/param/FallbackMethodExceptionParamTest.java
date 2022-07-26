package io.smallrye.faulttolerance.fallbackmethod.exception.param;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@FaultToleranceBasicTest
@WithSystemProperty(key = "smallrye.faulttolerance.mp-compatibility", value = "false")
public class FallbackMethodExceptionParamTest {
    @Test
    public void basic(BasicService service) {
        assertThat(service.doSomething()).isEqualTo("hello");
    }

    @Test
    public void generic(GenericService service) {
        assertThat(service.doSomething("ignored")).isEqualTo("hello");
    }

    @Test
    public void missing(MissingService service) {
        assertThatThrownBy(service::doSomething)
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("hello");
    }

    @Test
    public void mixed(MixedService service) {
        assertThat(service.doSomething(true)).isEqualTo("hello");
        assertThat(service.doSomething(false)).isEqualTo("fallback");
    }

    @Test
    public void multiple(MultipleService service) {
        assertThat(service.doSomething()).isEqualTo("hello");
    }

    @Test
    public void skipOn(SkipOnService service) {
        assertThatThrownBy(() -> service.doSomething(true))
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessage("skipped");
        assertThat(service.doSomething(false)).isEqualTo("hello");
    }

    @Test
    public void varargs(VarargsService service) {
        assertThat(service.doSomething("ignored")).isEqualTo("hello 0");
        assertThat(service.doSomething("ignored", 1)).isEqualTo("hello 1");
        assertThat(service.doSomething("ignored", 1, 2)).isEqualTo("hello 2");
    }
}
