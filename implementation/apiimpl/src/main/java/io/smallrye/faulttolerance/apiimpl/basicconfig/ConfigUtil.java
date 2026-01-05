package io.smallrye.faulttolerance.apiimpl.basicconfig;

import java.util.Optional;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import io.smallrye.faulttolerance.autoconfig.ConfigConstants;
import io.smallrye.faulttolerance.autoconfig.MethodDescriptor;

public final class ConfigUtil {
    private static final String OLD_FALLBACK = "Fallback/enabled";

    private static String newKey(String newSuffix, String configKey) {
        return ConfigConstants.PREFIX + "\"" + configKey + "\"." + newSuffix;
    }

    private static String oldKey(String oldSuffix, String configKey) {
        return configKey + "/" + oldSuffix;
    }

    private static String newKey(String newSuffix) {
        return ConfigConstants.PREFIX + ConfigConstants.GLOBAL + "." + newSuffix;
    }

    private static String oldKey(String oldSuffix) {
        return oldSuffix;
    }

    // ---

    public static boolean isEnabled(String newSuffix, String oldSuffix, MethodDescriptor method) {
        // TODO converting strings to boolean here is inconsistent,
        //  but it's how SmallRye Fault Tolerance has always done it

        Config config = ConfigProvider.getConfig();

        Optional<String> onMethodNew = config.getOptionalValue(
                newKey(newSuffix, method.declaringClass.getName() + "/" + method.name), String.class);
        if (onMethodNew.isPresent()) {
            return Boolean.parseBoolean(onMethodNew.get());
        }

        Optional<String> onMethod = config.getOptionalValue(
                oldKey(oldSuffix, method.declaringClass.getName() + "/" + method.name), String.class);
        if (onMethod.isPresent()) {
            return Boolean.parseBoolean(onMethod.get());
        }

        Optional<String> onClassNew = config.getOptionalValue(
                newKey(newSuffix, method.declaringClass.getName()), String.class);
        if (onClassNew.isPresent()) {
            return Boolean.parseBoolean(onClassNew.get());
        }

        Optional<String> onClass = config.getOptionalValue(
                oldKey(oldSuffix, method.declaringClass.getName()), String.class);
        if (onClass.isPresent()) {
            return Boolean.parseBoolean(onClass.get());
        }

        Optional<String> onGlobalNew = config.getOptionalValue(newKey(newSuffix), String.class);
        if (onGlobalNew.isPresent()) {
            return Boolean.parseBoolean(onGlobalNew.get());
        }

        Optional<String> onGlobal = config.getOptionalValue(oldKey(oldSuffix), String.class);
        if (onGlobal.isPresent()) {
            return Boolean.parseBoolean(onGlobal.get());
        }

        if (OLD_FALLBACK.equals(oldSuffix)) {
            return true;
        }

        Optional<Boolean> ftEnabledNew = config.getOptionalValue(ConfigConstants.PREFIX + ConfigConstants.ENABLED,
                Boolean.class);
        if (ftEnabledNew.isPresent()) {
            return ftEnabledNew.get();
        }

        Optional<Boolean> ftEnabled = config.getOptionalValue("MP_Fault_Tolerance_NonFallback_Enabled", Boolean.class);
        if (ftEnabled.isPresent()) {
            return ftEnabled.get();
        }

        return true;
    }

    public static boolean isEnabled(String newSuffix, String oldSuffix, String id) {
        // TODO converting strings to boolean here is inconsistent,
        //  but it's how SmallRye Fault Tolerance has always done it

        Config config = ConfigProvider.getConfig();

        Optional<String> identifiedNew = config.getOptionalValue(newKey(newSuffix, id), String.class);
        if (identifiedNew.isPresent()) {
            return Boolean.parseBoolean(identifiedNew.get());
        }

        Optional<String> identified = config.getOptionalValue(oldKey(oldSuffix, id), String.class);
        if (identified.isPresent()) {
            return Boolean.parseBoolean(identified.get());
        }

        Optional<String> globalNew = config.getOptionalValue(newKey(newSuffix), String.class);
        if (globalNew.isPresent()) {
            return Boolean.parseBoolean(globalNew.get());
        }

        Optional<String> global = config.getOptionalValue(oldKey(oldSuffix), String.class);
        if (global.isPresent()) {
            return Boolean.parseBoolean(global.get());
        }

        if (OLD_FALLBACK.equals(oldSuffix)) {
            return true;
        }

        Optional<Boolean> ftEnabledNew = config.getOptionalValue(ConfigConstants.PREFIX + ConfigConstants.ENABLED,
                Boolean.class);
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
