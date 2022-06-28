package io.smallrye.faulttolerance.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

public class WithSystemPropertyExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {
    @Override
    public void beforeAll(ExtensionContext context) {
        apply(context);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        apply(context);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        unapply(context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        unapply(context);
    }

    private static void apply(ExtensionContext context) {
        Map<String, String> map = AnnotationSupport.findRepeatableAnnotations(context.getElement(), WithSystemProperty.class)
                .stream()
                .collect(Collectors.toMap(WithSystemProperty::key, WithSystemProperty::value));

        getStore(context).put(getStoreKey(context), new Backup(map.keySet()));

        for (Map.Entry<String, String> entry : map.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }
    }

    private static void unapply(ExtensionContext context) {
        Backup backup = getStore(context).getOrDefault(getStoreKey(context), Backup.class, Backup.empty());
        backup.restore();
    }

    private static ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getStore(ExtensionContext.Namespace.create(WithSystemPropertyExtension.class));
    }

    private static Object getStoreKey(ExtensionContext context) {
        return context.getUniqueId();
    }

    private static class Backup {
        private final Set<String> toClear = new HashSet<>();
        private final Map<String, String> toReset = new HashMap<>();

        static Backup empty() {
            return new Backup(Collections.emptySet());
        }

        Backup(Set<String> keys) {
            for (String key : keys) {
                String value = System.getProperty(key);
                if (value == null) {
                    toClear.add(key);
                } else {
                    toReset.put(key, value);
                }
            }
        }

        public void restore() {
            for (String key : toClear) {
                System.clearProperty(key);
            }
            for (Map.Entry<String, String> entry : toReset.entrySet()) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }

    }
}
