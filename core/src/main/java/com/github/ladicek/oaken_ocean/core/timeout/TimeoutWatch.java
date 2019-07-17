package com.github.ladicek.oaken_ocean.core.timeout;

interface TimeoutWatch {
    boolean isRunning();

    void cancel();
}
