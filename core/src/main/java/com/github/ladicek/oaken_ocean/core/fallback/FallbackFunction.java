package com.github.ladicek.oaken_ocean.core.fallback;

@FunctionalInterface
public interface FallbackFunction<T> {
    T call(Throwable failure) throws Exception;
}
