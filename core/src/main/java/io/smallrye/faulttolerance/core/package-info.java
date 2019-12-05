/**
 * Core implementations of fault tolerance strategies conforming to MicroProfile Fault Tolerance.
 * The strategies here are not meant to be used directly.
 * Their API is not optimized for end user friendliness, but for integration correctness.
 * The core abstraction is {@link FaultToleranceStrategy}; each strategy implements that.
 */
package io.smallrye.faulttolerance.core;
