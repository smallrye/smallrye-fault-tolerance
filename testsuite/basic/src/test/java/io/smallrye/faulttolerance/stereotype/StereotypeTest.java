package io.smallrye.faulttolerance.stereotype;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class StereotypeTest {
    @Inject
    OnlyDirectStereotype onlyDirectStereotype;
    @Inject
    OnlyInheritedStereotype onlyInheritedStereotype;
    @Inject
    OnlyDirectTransitiveStereotype onlyDirectTransitiveStereotype;
    @Inject
    OnlyInheritedTransitiveStereotype onlyInheritedTransitiveStereotype;

    @Inject
    ClassOverridesDirectStereotype classOverridesDirectStereotype;
    @Inject
    ClassOverridesInheritedStereotype classOverridesInheritedStereotype;
    @Inject
    ClassOverridesDirectTransitiveStereotype classOverridesDirectTransitiveStereotype;
    @Inject
    ClassOverridesInheritedTransitiveStereotype classOverridesInheritedTransitiveStereotype;

    @Inject
    MethodOverridesDirectStereotype methodOverridesDirectStereotype;
    @Inject
    MethodOverridesInheritedStereotype methodOverridesInheritedStereotype;
    @Inject
    MethodOverridesDirectTransitiveStereotype methodOverridesDirectTransitiveStereotype;
    @Inject
    MethodOverridesInheritedTransitiveStereotype methodOverridesInheritedTransitiveStereotype;

    @Inject
    MethodOverridesClassAndDirectStereotype methodOverridesClassAndDirectStereotype;
    @Inject
    MethodOverridesClassAndInheritedStereotype methodOverridesClassAndInheritedStereotype;
    @Inject
    MethodOverridesClassAndDirectTransitiveStereotype methodOverridesClassAndDirectTransitiveStereotype;
    @Inject
    MethodOverridesClassAndInheritedTransitiveStereotype methodOverridesClassAndInheritedTransitiveStereotype;

    @Inject
    NonInheritedOnlyDirectStereotype nonInheritedOnlyDirectStereotype;
    @Inject
    NonInheritedOnlyInheritedStereotype nonInheritedOnlyInheritedStereotype;
    @Inject
    NonInheritedOnlyDirectTransitiveStereotype nonInheritedOnlyDirectTransitiveStereotype;
    @Inject
    NonInheritedOnlyInheritedTransitiveStereotype nonInheritedOnlyInheritedTransitiveStereotype;

    @BeforeEach
    void reset() {
        onlyDirectStereotype.reset();
        onlyInheritedStereotype.reset();
        onlyDirectTransitiveStereotype.reset();
        onlyInheritedTransitiveStereotype.reset();

        classOverridesDirectStereotype.reset();
        classOverridesInheritedStereotype.reset();
        classOverridesDirectTransitiveStereotype.reset();
        classOverridesInheritedTransitiveStereotype.reset();

        methodOverridesDirectStereotype.reset();
        methodOverridesInheritedStereotype.reset();
        methodOverridesDirectTransitiveStereotype.reset();
        methodOverridesInheritedTransitiveStereotype.reset();

        methodOverridesClassAndDirectStereotype.reset();
        methodOverridesClassAndInheritedStereotype.reset();
        methodOverridesClassAndDirectTransitiveStereotype.reset();
        methodOverridesClassAndInheritedTransitiveStereotype.reset();

        nonInheritedOnlyDirectStereotype.reset();
        nonInheritedOnlyInheritedStereotype.reset();
        nonInheritedOnlyDirectTransitiveStereotype.reset();
        nonInheritedOnlyInheritedTransitiveStereotype.reset();
    }

    @Test
    void onlyDirectStereotype() {
        assertThat(onlyDirectStereotype.hello()).isEqualTo("fallback");
        assertThat(onlyDirectStereotype.getInvocations()).isEqualTo(5);
    }

    @Test
    void onlyInheritedStereotype() {
        assertThat(onlyInheritedStereotype.hello()).isEqualTo("fallback");
        assertThat(onlyInheritedStereotype.getInvocations()).isEqualTo(5);
    }

    @Test
    void onlyDirectTransitiveStereotype() {
        assertThat(onlyDirectTransitiveStereotype.hello()).isEqualTo("fallback");
        assertThat(onlyDirectTransitiveStereotype.getInvocations()).isEqualTo(5);
    }

    @Test
    void onlyInheritedTransitiveStereotype() {
        assertThat(onlyInheritedTransitiveStereotype.hello()).isEqualTo("fallback");
        assertThat(onlyInheritedTransitiveStereotype.getInvocations()).isEqualTo(5);
    }

    @Test
    void classOverridesDirectStereotype() {
        assertThat(classOverridesDirectStereotype.hello()).isEqualTo("fallback");
        assertThat(classOverridesDirectStereotype.getInvocations()).isEqualTo(6);
    }

    @Test
    void classOverridesInheritedStereotype() {
        assertThat(classOverridesInheritedStereotype.hello()).isEqualTo("fallback");
        assertThat(classOverridesInheritedStereotype.getInvocations()).isEqualTo(6);
    }

    @Test
    void classOverridesDirectTransitiveStereotype() {
        assertThat(classOverridesDirectTransitiveStereotype.hello()).isEqualTo("fallback");
        assertThat(classOverridesDirectTransitiveStereotype.getInvocations()).isEqualTo(6);
    }

    @Test
    void classOverridesInheritedTransitiveStereotype() {
        assertThat(classOverridesInheritedTransitiveStereotype.hello()).isEqualTo("fallback");
        assertThat(classOverridesInheritedTransitiveStereotype.getInvocations()).isEqualTo(6);
    }

    @Test
    void methodOverridesDirectStereotype() {
        assertThat(methodOverridesDirectStereotype.hello()).isEqualTo("fallback");
        assertThat(methodOverridesDirectStereotype.getInvocations()).isEqualTo(7);
    }

    @Test
    void methodOverridesInheritedStereotype() {
        assertThat(methodOverridesInheritedStereotype.hello()).isEqualTo("fallback");
        assertThat(methodOverridesInheritedStereotype.getInvocations()).isEqualTo(7);
    }

    @Test
    void methodOverridesDirectTransitiveStereotype() {
        assertThat(methodOverridesDirectTransitiveStereotype.hello()).isEqualTo("fallback");
        assertThat(methodOverridesDirectTransitiveStereotype.getInvocations()).isEqualTo(7);
    }

    @Test
    void methodOverridesInheritedTransitiveStereotype() {
        assertThat(methodOverridesInheritedTransitiveStereotype.hello()).isEqualTo("fallback");
        assertThat(methodOverridesInheritedTransitiveStereotype.getInvocations()).isEqualTo(7);
    }

    @Test
    void methodOverridesClassAndDirectStereotype() {
        assertThat(methodOverridesClassAndDirectStereotype.hello()).isEqualTo("fallback");
        assertThat(methodOverridesClassAndDirectStereotype.getInvocations()).isEqualTo(7);
    }

    @Test
    void methodOverridesClassAndInheritedStereotype() {
        assertThat(methodOverridesClassAndInheritedStereotype.hello()).isEqualTo("fallback");
        assertThat(methodOverridesClassAndInheritedStereotype.getInvocations()).isEqualTo(7);
    }

    @Test
    void methodOverridesClassAndDirectTransitiveStereotype() {
        assertThat(methodOverridesClassAndDirectTransitiveStereotype.hello()).isEqualTo("fallback");
        assertThat(methodOverridesClassAndDirectTransitiveStereotype.getInvocations()).isEqualTo(7);
    }

    @Test
    void methodOverridesClassAndInheritedTransitiveStereotype() {
        assertThat(methodOverridesClassAndInheritedTransitiveStereotype.hello()).isEqualTo("fallback");
        assertThat(methodOverridesClassAndInheritedTransitiveStereotype.getInvocations()).isEqualTo(7);
    }

    @Test
    void nonInheritedOnlyDirectStereotype() {
        assertThat(nonInheritedOnlyDirectStereotype.hello()).isEqualTo("fallback");
        assertThat(nonInheritedOnlyDirectStereotype.getInvocations()).isEqualTo(8);
    }

    @Test
    void nonInheritedOnlyInheritedStereotype() {
        assertThat(nonInheritedOnlyInheritedStereotype.hello()).isEqualTo("fallback");
        assertThat(nonInheritedOnlyInheritedStereotype.getInvocations()).isEqualTo(1);
    }

    @Test
    void nonInheritedOnlyDirectTransitiveStereotype() {
        assertThat(nonInheritedOnlyDirectTransitiveStereotype.hello()).isEqualTo("fallback");
        assertThat(nonInheritedOnlyDirectTransitiveStereotype.getInvocations()).isEqualTo(8);
    }

    @Test
    void nonInheritedOnlyInheritedTransitiveStereotype() {
        assertThat(nonInheritedOnlyInheritedTransitiveStereotype.hello()).isEqualTo("fallback");
        assertThat(nonInheritedOnlyInheritedTransitiveStereotype.getInvocations()).isEqualTo(1);
    }
}
