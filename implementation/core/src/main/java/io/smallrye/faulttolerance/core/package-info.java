/**
 * Core implementations of fault tolerance strategies conforming to MicroProfile Fault Tolerance.
 * The strategies here are not meant to be used directly.
 * Their API is not optimized for end user friendliness, but for integration correctness.
 * The core abstraction is {@link io.smallrye.faulttolerance.core.FaultToleranceStrategy}; each strategy implements that.
 * API stability is <b>not</b> guaranteed!
 */
package io.smallrye.faulttolerance.core;
