package io.smallrye.faulttolerance.stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

import jakarta.enterprise.inject.Stereotype;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

@Stereotype
@Retry(maxRetries = 2, delay = 50, delayUnit = ChronoUnit.MILLIS)
@CircuitBreaker(requestVolumeThreshold = 4, failureRatio = 0.5, delay = 5000)
@Timeout(value = 2, unit = ChronoUnit.SECONDS)
@Bulkhead(value = 10)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface FtStereotype {
}
