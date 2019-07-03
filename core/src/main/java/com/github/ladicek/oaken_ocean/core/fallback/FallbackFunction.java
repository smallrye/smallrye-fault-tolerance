package com.github.ladicek.oaken_ocean.core.fallback;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 03/07/2019
 */
@FunctionalInterface
public interface FallbackFunction<T> {
    T call(Throwable failure) throws Exception;
}
