package io.smallrye.faulttolerance.async.sizing;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;

import io.smallrye.faulttolerance.core.util.party.Party;

@ApplicationScoped
public class HelloService {

    @Asynchronous
    public CompletionStage<String> hello(Party.Participant participant) throws InterruptedException {
        participant.attend();
        return completedFuture("hello");
    }
}
