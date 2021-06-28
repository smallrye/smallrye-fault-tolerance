package io.smallrye.faulttolerance.autoconfig;

import java.lang.annotation.Annotation;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Fallback;

public interface Config {
    // can't be generated, must be implemented (with a default method) in the config interface
    void validate();

    // ---

    Class<?> beanClass();

    MethodDescriptor method();

    // defined by Annotation, so for convenience, we expose it here too
    Class<? extends Annotation> annotationType();

    // ---

    static <A extends Annotation> boolean isEnabled(Class<A> annotationType, MethodDescriptor method) {
        // TODO converting strings to boolean here is inconsistent,
        //  but it's how SmallRye Fault Tolerance has always done it

        org.eclipse.microprofile.config.Config config = ConfigProvider.getConfig();

        Optional<String> onMethod = config.getOptionalValue(method.declaringClass.getName() +
                "/" + method.name + "/" + annotationType.getSimpleName() + "/enabled", String.class);
        if (onMethod.isPresent()) {
            return Boolean.parseBoolean(onMethod.get());
        }

        Optional<String> onClass = config.getOptionalValue(method.declaringClass.getName() +
                "/" + annotationType.getSimpleName() + "/enabled", String.class);
        if (onClass.isPresent()) {
            return Boolean.parseBoolean(onClass.get());
        }

        Optional<String> onGlobal = config.getOptionalValue(annotationType.getSimpleName()
                + "/enabled", String.class);
        if (onGlobal.isPresent()) {
            return Boolean.parseBoolean(onGlobal.get());
        }

        if (Fallback.class.equals(annotationType)) {
            return true;
        }

        return config.getOptionalValue("MP_Fault_Tolerance_NonFallback_Enabled", Boolean.class)
                .orElse(true);
    }
}
