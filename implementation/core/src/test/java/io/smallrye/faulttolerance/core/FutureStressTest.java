package io.smallrye.faulttolerance.core;

import static io.smallrye.faulttolerance.core.util.Action.startThread;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.core.util.Action;
import io.smallrye.faulttolerance.core.util.barrier.Barrier;
import io.smallrye.faulttolerance.core.util.party.Party;

public class FutureStressTest {
    @Test
    public void test() throws InterruptedException {
        for (int i = 0; i < 20_000; i++) {
            Completer<String> completer = Completer.create();
            AtomicReference<String> result = new AtomicReference<>();
            AtomicInteger callbackCounter = new AtomicInteger();

            Party party = Party.create(2);
            Barrier resultBarrier = Barrier.noninterruptible();

            Action completerAction = () -> {
                party.participant().attend();
                completer.complete("foobar");
            };
            Action callbackAction = () -> {
                party.participant().attend();
                completer.future().then((value, error) -> {
                    result.set(value);
                    callbackCounter.incrementAndGet();
                    resultBarrier.open();
                });
            };

            // in practice, the thread that starts first has an advantage
            // this makes sure that the advantage is evenly distributed
            if (ThreadLocalRandom.current().nextBoolean()) {
                startThread(callbackAction);
                startThread(completerAction);
            } else {
                startThread(completerAction);
                startThread(callbackAction);
            }

            party.organizer().waitForAll();
            party.organizer().disband();
            resultBarrier.await();
            assertThat(result).hasValue("foobar");
            assertThat(callbackCounter).hasValue(1);
        }
    }
}
