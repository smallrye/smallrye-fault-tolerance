package io.smallrye.faulttolerance.api;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class GuardBuilderConsistencyTest {
    private final Map<Class<?>, Class<?>> types = Map.of(
            Guard.Builder.class,
            TypedGuard.Builder.class,

            Guard.Builder.BulkheadBuilder.class,
            TypedGuard.Builder.BulkheadBuilder.class,

            Guard.Builder.CircuitBreakerBuilder.class,
            TypedGuard.Builder.CircuitBreakerBuilder.class,

            // no `FallbackBuilder`, that only exists on `TypedGuard`

            Guard.Builder.RateLimitBuilder.class,
            TypedGuard.Builder.RateLimitBuilder.class,

            Guard.Builder.RetryBuilder.class,
            TypedGuard.Builder.RetryBuilder.class,

            Guard.Builder.RetryBuilder.CustomBackoffBuilder.class,
            TypedGuard.Builder.RetryBuilder.CustomBackoffBuilder.class,

            Guard.Builder.RetryBuilder.ExponentialBackoffBuilder.class,
            TypedGuard.Builder.RetryBuilder.ExponentialBackoffBuilder.class,

            Guard.Builder.RetryBuilder.FibonacciBackoffBuilder.class,
            TypedGuard.Builder.RetryBuilder.FibonacciBackoffBuilder.class,

            Guard.Builder.TimeoutBuilder.class,
            TypedGuard.Builder.TimeoutBuilder.class);

    @Test
    public void test() throws NoSuchMethodException {
        for (Map.Entry<Class<?>, Class<?>> entry : types.entrySet()) {
            Class<?> guardClass = entry.getKey();
            Class<?> typedGuardClass = entry.getValue();

            for (Method guardMethod : guardClass.getDeclaredMethods()) {
                typedGuardClass.getDeclaredMethod(guardMethod.getName(), guardMethod.getParameterTypes());
            }

            for (Method typedGuardMethod : typedGuardClass.getDeclaredMethods()) {
                if (TypedGuard.Builder.class.equals(typedGuardClass)
                        && "withFallback".equalsIgnoreCase(typedGuardMethod.getName())) {
                    continue;
                }

                guardClass.getDeclaredMethod(typedGuardMethod.getName(), typedGuardMethod.getParameterTypes());
            }
        }
    }
}
