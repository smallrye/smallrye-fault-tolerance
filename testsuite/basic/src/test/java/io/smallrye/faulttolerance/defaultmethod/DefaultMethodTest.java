package io.smallrye.faulttolerance.defaultmethod;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddExtensions(InterfaceBasedExtension.class)
@AddBeanClasses(SimpleService.class)
public class DefaultMethodTest {
    @Test
    public void test(HelloService service) {
        assertThat(service.hello()).isEqualTo("Hello, world!");
    }
}
