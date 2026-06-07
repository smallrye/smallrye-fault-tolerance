package io.smallrye.faulttolerance.stereotype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

import jakarta.enterprise.inject.Stereotype;

import org.eclipse.microprofile.faulttolerance.Retry;

@Stereotype
@Retry(maxRetries = 2, delay = 50, delayUnit = ChronoUnit.MILLIS)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RetryStereotype {
}
