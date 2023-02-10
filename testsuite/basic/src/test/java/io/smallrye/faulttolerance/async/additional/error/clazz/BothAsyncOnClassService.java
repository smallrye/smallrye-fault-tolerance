package io.smallrye.faulttolerance.async.additional.error.clazz;

import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;

@Dependent
@Asynchronous
@AsynchronousNonBlocking
public class BothAsyncOnClassService {
    @Retry
    public CompletionStage<String> hello() {
        throw new IllegalArgumentException();
    }
}
