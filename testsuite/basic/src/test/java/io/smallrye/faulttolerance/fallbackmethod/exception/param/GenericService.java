package io.smallrye.faulttolerance.fallbackmethod.exception.param;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GenericService extends GenericSuperclass<String> {
    @Override
    public String fallback(String param, IllegalArgumentException e) {
        return e.getMessage();
    }
}
