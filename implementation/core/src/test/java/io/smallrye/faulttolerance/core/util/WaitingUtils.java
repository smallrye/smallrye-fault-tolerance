package io.smallrye.faulttolerance.core.util;

import static org.junit.Assert.fail;

import java.util.function.Supplier;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public abstract class WaitingUtils {

    public static void assertThatWithin(int timeoutMs, String message, Supplier<Boolean> test) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted waiting for " + message);
            }
            if (test.get()) {
                return;
            }
        }
        fail(message + " not satisfied in " + timeoutMs + "ms");
    }

    private WaitingUtils() {
    }
}
