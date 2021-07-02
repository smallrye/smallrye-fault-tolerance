package io.smallrye.faulttolerance.async.additional.error.clazz;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;

@Dependent
@Blocking
@NonBlocking
public class BlockingNonBlockingOnClassService {
    @Retry
    public CompletionStage<String> hello() {
        throw new IllegalArgumentException();
    }
}
