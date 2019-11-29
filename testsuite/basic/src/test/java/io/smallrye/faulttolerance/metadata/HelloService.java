package io.smallrye.faulttolerance.metadata;

import org.eclipse.microprofile.faulttolerance.Retry;

@Retry(maxRetries = 4)
public class HelloService extends BaseService {

}
