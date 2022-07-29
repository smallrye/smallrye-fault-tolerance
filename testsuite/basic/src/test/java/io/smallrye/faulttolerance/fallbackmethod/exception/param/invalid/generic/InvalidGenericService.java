package io.smallrye.faulttolerance.fallbackmethod.exception.param.invalid.generic;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InvalidGenericService extends InvalidGenericSuperclass<String> {
    public String fallback(String param, IllegalArgumentException e) {
        return e.getMessage();
    }
}
