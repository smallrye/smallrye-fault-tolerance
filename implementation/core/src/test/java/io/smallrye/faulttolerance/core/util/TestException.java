package io.smallrye.faulttolerance.core.util;

import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

public class TestException extends Exception {
    public TestException() {
    }

    // generic return type so that reference to this method can be used as `j.u.f.Supplier`
    public static <V> V doThrow() {
        throw sneakyThrow(new TestException());
    }
}
