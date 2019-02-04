package com.github.ladicek.oaken_ocean.core.retry;

public enum NoDelay implements Delay { // enum-singleton
    INSTANCE;

    @Override
    public void sleep() {
    }
}
