package io.smallrye.faulttolerance;

import com.netflix.hystrix.*;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherCollapser;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherCommand;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherThreadPool;

public class FaultToleranceMetricsPublisher extends HystrixMetricsPublisher {


    @Override
    public HystrixMetricsPublisherCommand getMetricsPublisherForCommand(HystrixCommandKey commandKey, HystrixCommandGroupKey commandGroupKey, HystrixCommandMetrics metrics, HystrixCircuitBreaker circuitBreaker, HystrixCommandProperties properties) {
        System.out.println("**** Here ****");
        return super.getMetricsPublisherForCommand(commandKey, commandGroupKey, metrics, circuitBreaker, properties);
    }

    @Override
    public HystrixMetricsPublisherThreadPool getMetricsPublisherForThreadPool(HystrixThreadPoolKey threadPoolKey, HystrixThreadPoolMetrics metrics, HystrixThreadPoolProperties properties) {
        System.out.println("**** There ****");
        return super.getMetricsPublisherForThreadPool(threadPoolKey, metrics, properties);
    }

    @Override
    public HystrixMetricsPublisherCollapser getMetricsPublisherForCollapser(HystrixCollapserKey collapserKey, HystrixCollapserMetrics metrics, HystrixCollapserProperties properties) {
        System.out.println("**** Here too ****");
        return super.getMetricsPublisherForCollapser(collapserKey, metrics, properties);
    }

    public FaultToleranceMetricsPublisher() {
        super();
        System.out.println("######## Registering Metrics Publisher ########");
    }
}
