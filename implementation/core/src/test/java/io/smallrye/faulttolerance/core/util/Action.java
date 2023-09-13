package io.smallrye.faulttolerance.core.util;

import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

public interface Action {
    void run() throws Exception;

    static void startThread(Action action) {
        new Thread(() -> {
            try {
                action.run();
            } catch (Exception e) {
                sneakyThrow(e);
            }
        }).start();
    }
}
