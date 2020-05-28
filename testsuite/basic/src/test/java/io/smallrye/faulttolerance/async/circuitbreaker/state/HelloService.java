package io.smallrye.faulttolerance.async.circuitbreaker.state;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Singleton;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import io.smallrye.faulttolerance.api.CircuitBreakerState;
import io.smallrye.faulttolerance.api.CircuitBreakerStateChanged;

@ApplicationScoped
public class HelloService {
    static final String OK = "Hello";
    static final int THRESHOLD = 5;
    static final int DELAY = 500;

    @CircuitBreaker(requestVolumeThreshold = THRESHOLD, delay = DELAY)
    public String hello(Exception exception) throws Exception {
        if (exception != null) {
            throw exception;
        }

        return OK;
    }

    @Singleton
    public static class CB {
        CircuitBreakerState state = CircuitBreakerState.CLOSED;

        public void onStateChange(@Observes CircuitBreakerStateChanged event) {
            if (HelloService.class.equals(event.clazz) && "hello".equals(event.method.getName())) {
                state = event.targetState;
            }
        }
    }
}
