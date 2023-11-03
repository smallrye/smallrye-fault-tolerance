package io.smallrye.faulttolerance.standalone;

import java.util.concurrent.ExecutorService;

public interface Configuration {
    default boolean enabled() {
        return true;
    }

    ExecutorService executor();

    default void onShutdown() throws InterruptedException {
    }
}
