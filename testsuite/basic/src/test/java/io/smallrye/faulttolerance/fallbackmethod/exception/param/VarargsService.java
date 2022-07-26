package io.smallrye.faulttolerance.fallbackmethod.exception.param;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Fallback;

@ApplicationScoped
public class VarargsService {
    @Fallback(fallbackMethod = "fallback")
    public String doSomething(String str, int... intArray) {
        throw new IllegalArgumentException("hello " + intArray.length);
    }

    public String fallback(String str, int[] intArray, Exception e) {
        return e.getMessage();
    }
}
