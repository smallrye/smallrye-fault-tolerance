package io.smallrye.faulttolerance.core.util;

public class TestException extends Exception {
    public TestException() {
    }

    // generic return type so that reference to this method can be used as `j.u.c.Callable`
    public static <V> V doThrow() throws TestException {
        throw new TestException();
    }
}
