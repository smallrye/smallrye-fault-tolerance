package io.smallrye.faulttolerance.retry.beforeretry.call.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class BeforeRetryCallContextTest {
    @BeforeEach
    public void clearRecordedContext() {
        BeforeRetryCallContextService.clear();
    }

    @Test
    public void retryOnFailure(BeforeRetryCallContextService service) {
        assertThatThrownBy(() -> service.onFailure("argument"))
                .isExactlyInstanceOf(IllegalArgumentException.class);

        assertThat(BeforeRetryCallContextService.attempts).hasValue(3); // 1 initial invocation + 2 retries
        assertThat(BeforeRetryCallContextService.methods).containsExactly("onFailure", "onFailure");
        assertThat(BeforeRetryCallContextService.parameters)
                .hasSize(2)
                .allMatch(parameters -> parameters.equals(List.of("argument")));
        assertThat(BeforeRetryCallContextService.results).containsExactly(null, null);
        assertThat(BeforeRetryCallContextService.failures)
                .hasSize(2)
                .allMatch(failure -> failure instanceof IllegalArgumentException);
    }

    @Test
    public void retryOnResult(BeforeRetryCallContextService service) {
        assertThat(service.onResult()).isEqualTo("good");

        assertThat(BeforeRetryCallContextService.attempts).hasValue(4); // 1 initial invocation + 3 retries
        assertThat(BeforeRetryCallContextService.methods).containsExactly("onResult", "onResult", "onResult");
        // the handler sees the result of the previous invocation which was considered a failure
        assertThat(BeforeRetryCallContextService.results).containsExactly("bad", "bad", "bad");
        assertThat(BeforeRetryCallContextService.failures).hasSize(3).allMatch(Objects::isNull);
    }

    @Test
    public void retryOnRejectedNullResult(BeforeRetryCallContextService service) {
        assertThat(service.onNullResult()).isEqualTo("good");

        assertThat(BeforeRetryCallContextService.attempts).hasValue(3); // 1 initial invocation + 2 retries
        // a rejected `null` result means both the result and the failure are `null`
        assertThat(BeforeRetryCallContextService.results).containsExactly(null, null);
        assertThat(BeforeRetryCallContextService.failures).containsExactly(null, null);
    }

    @Test
    public void retryOnResultAsync(BeforeRetryCallContextService service) throws Exception {
        assertThat(service.onResultAsync().toCompletableFuture().get(10, TimeUnit.SECONDS)).isEqualTo("good");

        assertThat(BeforeRetryCallContextService.attempts).hasValue(4); // 1 initial invocation + 3 retries
        // the handler must see the value the `CompletionStage` produced, not the `CompletionStage` itself
        assertThat(BeforeRetryCallContextService.results)
                .hasSize(3)
                .containsExactly("bad", "bad", "bad");
        assertThat(BeforeRetryCallContextService.failures).hasSize(3).allMatch(Objects::isNull);
    }

    @Test
    public void retryOnPrimitiveResult(BeforeRetryCallContextService service) {
        assertThat(service.onPrimitiveResult()).isTrue();

        assertThat(BeforeRetryCallContextService.attempts).hasValue(4); // 1 initial invocation + 3 retries
        assertThat(BeforeRetryCallContextService.methods)
                .containsExactly("onPrimitiveResult", "onPrimitiveResult", "onPrimitiveResult");
        // the result of the primitive-returning invocation is boxed
        assertThat(BeforeRetryCallContextService.results).containsExactly(false, false, false);
        assertThat(BeforeRetryCallContextService.failures).hasSize(3).allMatch(Objects::isNull);
    }

    @Test
    public void retryOnPrimitiveFailure(BeforeRetryCallContextService service) {
        assertThatThrownBy(service::onPrimitiveFailure)
                .isExactlyInstanceOf(IllegalArgumentException.class);

        assertThat(BeforeRetryCallContextService.attempts).hasValue(3); // 1 initial invocation + 2 retries
        // an invocation that failed has no result; it is `null`, not the default value of the return type
        assertThat(BeforeRetryCallContextService.results).containsExactly(null, null);
        assertThat(BeforeRetryCallContextService.failures)
                .hasSize(2)
                .allMatch(failure -> failure instanceof IllegalArgumentException);
    }

    @Test
    public void retryOnVoidFailure(BeforeRetryCallContextService service) {
        assertThatThrownBy(service::onVoidFailure)
                .isExactlyInstanceOf(IllegalArgumentException.class);

        assertThat(BeforeRetryCallContextService.attempts).hasValue(3); // 1 initial invocation + 2 retries
        assertThat(BeforeRetryCallContextService.results).containsExactly(null, null);
        assertThat(BeforeRetryCallContextService.failures)
                .hasSize(2)
                .allMatch(failure -> failure instanceof IllegalArgumentException);
    }
}
