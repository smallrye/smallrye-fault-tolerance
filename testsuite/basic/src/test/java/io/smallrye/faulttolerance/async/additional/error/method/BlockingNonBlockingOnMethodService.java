package io.smallrye.faulttolerance.async.additional.error.method;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;

@Dependent
public class BlockingNonBlockingOnMethodService {
    @Retry
    @Blocking
    @NonBlocking
    public CompletionStage<String> hello() {
        throw new IllegalArgumentException();
    }
}
