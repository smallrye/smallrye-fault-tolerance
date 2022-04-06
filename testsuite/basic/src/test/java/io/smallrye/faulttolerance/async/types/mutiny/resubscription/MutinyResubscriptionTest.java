package io.smallrye.faulttolerance.async.types.mutiny.resubscription;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.mutiny.Uni;

@FaultToleranceBasicTest
public class MutinyResubscriptionTest {
    // this test verifies resubscription, which is triggered via retry

    @Test
    public void test(HelloService service) {
        Uni<String> hello = service.hello()
                .onFailure().retry().atMost(2)
                .onFailure().recoverWithItem("hello");
        assertThat(hello.await().indefinitely()).isEqualTo("hello");

        // the service.hello() method has @Retry with default settings, so 1 initial attempt + 3 retries = 4 total
        // the onFailure().retry() handler does 1 initial attempt + 2 retries = 3 total
        assertThat(HelloService.COUNTER).hasValue(4 * 3);
    }
}
