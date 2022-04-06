package io.smallrye.faulttolerance.async.types.rxjava.resubscription;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.reactivex.rxjava3.core.Maybe;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class RxjavaResubscriptionTest {
    // this test verifies resubscription, which is triggered via retry

    @Test
    public void test(HelloService service) {
        Maybe<String> hello = service.hello()
                .retry(2)
                .onErrorReturnItem("hello");
        assertThat(hello.blockingGet()).isEqualTo("hello");

        // the service.hello() method has @Retry with default settings, so 1 initial attempt + 3 retries = 4 total
        // the retry() handler does 1 initial attempt + 2 retries = 3 total
        assertThat(HelloService.COUNTER).hasValue(4 * 3);
    }
}
