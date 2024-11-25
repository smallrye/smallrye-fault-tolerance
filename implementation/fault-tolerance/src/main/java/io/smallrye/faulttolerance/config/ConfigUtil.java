package io.smallrye.faulttolerance.config;

import java.lang.annotation.Annotation;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Fallback;

import io.smallrye.faulttolerance.autoconfig.MethodDescriptor;

final class ConfigUtil {
    static final String ENABLED = "enabled";
    static final String GLOBAL = "global";

    static String newKey(Class<? extends Annotation> annotation, String member, Class<?> declaringClass, String method) {
        return ConfigPrefix.VALUE + "\"" + declaringClass.getName() + "/" + method + "\"."
                + NewConfig.get(annotation, member);
    }

    static String oldKey(Class<? extends Annotation> annotation, String member, Class<?> declaringClass, String method) {
        return declaringClass.getName() + "/" + method + "/" + annotation.getSimpleName() + "/" + member;
    }

    static String newKey(Class<? extends Annotation> annotation, String member, Class<?> declaringClass) {
        return ConfigPrefix.VALUE + "\"" + declaringClass.getName() + "\"."
                + NewConfig.get(annotation, member);
    }

    static String oldKey(Class<? extends Annotation> annotation, String member, Class<?> declaringClass) {
        return declaringClass.getName() + "/" + annotation.getSimpleName() + "/" + member;
    }

    static String newKey(Class<? extends Annotation> annotation, String member) {
        return ConfigPrefix.VALUE + GLOBAL + "." + NewConfig.get(annotation, member);
    }

    static String oldKey(Class<? extends Annotation> annotation, String member) {
        return annotation.getSimpleName() + "/" + member;
    }

    // ---

    static boolean isEnabled(Class<? extends Annotation> annotationType, MethodDescriptor method) {
        // TODO converting strings to boolean here is inconsistent,
        //  but it's how SmallRye Fault Tolerance has always done it

        org.eclipse.microprofile.config.Config config = ConfigProvider.getConfig();

        Optional<String> onMethodNew = config.getOptionalValue(
                newKey(annotationType, ENABLED, method.declaringClass, method.name), String.class);
        if (onMethodNew.isPresent()) {
            return Boolean.parseBoolean(onMethodNew.get());
        }

        Optional<String> onMethod = config.getOptionalValue(
                oldKey(annotationType, ENABLED, method.declaringClass, method.name), String.class);
        if (onMethod.isPresent()) {
            return Boolean.parseBoolean(onMethod.get());
        }

        Optional<String> onClassNew = config.getOptionalValue(
                newKey(annotationType, ENABLED, method.declaringClass), String.class);
        if (onClassNew.isPresent()) {
            return Boolean.parseBoolean(onClassNew.get());
        }

        Optional<String> onClass = config.getOptionalValue(
                oldKey(annotationType, ENABLED, method.declaringClass), String.class);
        if (onClass.isPresent()) {
            return Boolean.parseBoolean(onClass.get());
        }

        Optional<String> onGlobalNew = config.getOptionalValue(newKey(annotationType, ENABLED), String.class);
        if (onGlobalNew.isPresent()) {
            return Boolean.parseBoolean(onGlobalNew.get());
        }

        Optional<String> onGlobal = config.getOptionalValue(oldKey(annotationType, ENABLED), String.class);
        if (onGlobal.isPresent()) {
            return Boolean.parseBoolean(onGlobal.get());
        }

        if (Fallback.class.equals(annotationType)) {
            return true;
        }

        Optional<Boolean> ftEnabledNew = config.getOptionalValue(ConfigPrefix.VALUE + ENABLED, Boolean.class);
        if (ftEnabledNew.isPresent()) {
            return ftEnabledNew.get();
        }

        Optional<Boolean> ftEnabled = config.getOptionalValue("MP_Fault_Tolerance_NonFallback_Enabled", Boolean.class);
        if (ftEnabled.isPresent()) {
            return ftEnabled.get();
        }

        return true;
    }
}
