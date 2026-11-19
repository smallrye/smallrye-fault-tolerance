package io.smallrye.faulttolerance.api;

import org.eclipse.microprofile.faulttolerance.ExecutionContext;

import io.smallrye.common.annotation.Experimental;

/**
 * Context of a guarded method invocation. Extends MicroProfile Fault Tolerance's
 * {@link ExecutionContext} and provides access to the returned result.
 * <p>
 * If the guarded method returns a value, {@link #getResult()} returns it and {@link #getFailure()}
 * returns {@code null}. If the guarded method fails with an exception, {@link #getResult()} returns
 * {@code null}. Note that {@code null} is a valid result, so to find out whether the guarded method
 * returned a value or failed, check {@code getFailure() == null}.
 */
@Experimental("first attempt at supporting Retry-After delay extraction")
public interface CallContext extends ExecutionContext {
    /**
     * Returns the result of the guarded method execution, or {@code null} if the method failed.
     * <p>
     * For asynchronous methods, this is the value that the asynchronous type eventually produces,
     * not a value of the asynchronous type itself. For example, if the method returns
     * {@code CompletionStage<String>}, this method returns a {@code String}.
     *
     * @return the result returned by the guarded method
     */
    Object getResult();
}
