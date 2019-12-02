package com.github.ladicek.oaken_ocean.core;

import java.util.concurrent.Callable;

/**
 * A context of a fault tolerance operation.
 * Wraps the call that should be performed in a fault tolerant manner.
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public interface InvocationContext<V> {
    Callable<V> getDelegate();
}
