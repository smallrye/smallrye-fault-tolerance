package io.smallrye.faulttolerance.interfaces.fallback;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.weld.junit5.auto.AddBeanClasses;
import org.jboss.weld.junit5.auto.AddExtensions;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddExtensions(InterfaceBasedExtension.class)
@AddBeanClasses({ PublicHello.class, PackagePrivateHello.class, PublicPing.class, PackagePrivatePing.class })
public class FallbackOnInterfaceTest {
    @Test
    public void defaultMethods(HelloService service) {
        assertThat(service.hello()).isEqualTo("Hello, world!");
    }

    @Test
    public void privateMethods(PingService service) {
        assertThat(service.ping()).isEqualTo("Ping! Pong!");
    }
}
