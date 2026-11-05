package io.smallrye.faulttolerance.bulkhead.requestcontext;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class CorrelationId {
    private String value;

    public String get() {
        return value;
    }

    public void set(String value) {
        this.value = value;
    }
}
