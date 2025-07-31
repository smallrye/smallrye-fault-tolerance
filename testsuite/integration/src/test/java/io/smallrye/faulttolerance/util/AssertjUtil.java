package io.smallrye.faulttolerance.util;

import java.util.function.Predicate;

import org.assertj.core.api.Condition;

public class AssertjUtil {
    public static <T> Condition<T> condition(Predicate<T> predicate) {
        return new Condition<>(predicate, "");
    }
}
