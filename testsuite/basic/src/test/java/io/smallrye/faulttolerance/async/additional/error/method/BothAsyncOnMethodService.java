package io.smallrye.faulttolerance.async.additional.error.method;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;

@Dependent
public class BothAsyncOnMethodService {
    @Retry
    @Asynchronous
    @AsynchronousNonBlocking
    public CompletionStage<String> hello() {
        throw new IllegalArgumentException();
    }
}
