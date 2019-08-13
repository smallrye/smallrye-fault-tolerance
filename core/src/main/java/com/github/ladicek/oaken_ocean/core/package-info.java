/**
 * Core implementations of fault tolerance strategies conforming to MicroProfile Fault Tolerance.
 * The strategies here are not meant to be used directly.
 * Their API is not optimized for end user friendliness, but for integration correctness.
 * The core abstraction is {@link java.util.concurrent.Callable}; each strategy implements that.
 * The strategies must be thread-safe, as they are expected to be used simultaneously from multiple threads.
 */
package com.github.ladicek.oaken_ocean.core;
