package io.smallrye.faulttolerance.metadata;

import jakarta.enterprise.context.Dependent;

import org.eclipse.microprofile.faulttolerance.Retry;

@Dependent
@Retry(maxRetries = 4)
public class HelloService extends BaseService {

}
