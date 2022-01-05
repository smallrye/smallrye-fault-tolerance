package io.smallrye.faulttolerance.fallback.handler.primitive;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class PrimitiveFallbackHandlerTest {
    @Test
    public void voidFallback(MyService service) {
        assertDoesNotThrow(service::doSomething);
        assertTrue(MyVoidFallbackHandler.invoked);
    }

    @Test
    public void intFallback(MyService service) {
        int result = assertDoesNotThrow(service::returnSomething);
        assertEquals(42, result);
        assertTrue(MyIntFallbackHandler.invoked);
    }
}
