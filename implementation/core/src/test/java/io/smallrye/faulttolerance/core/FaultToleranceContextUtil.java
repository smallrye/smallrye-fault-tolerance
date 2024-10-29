package io.smallrye.faulttolerance.core;

import java.util.concurrent.Callable;

public class FaultToleranceContextUtil {
    public static <T> FaultToleranceContext<T> sync(Callable<T> callable) {
        return new FaultToleranceContext<>(() -> Future.from(callable), false);
    }

    public static <T> FaultToleranceContext<T> async(Callable<T> callable) {
        return new FaultToleranceContext<>(() -> Future.from(callable), true);
    }
}
