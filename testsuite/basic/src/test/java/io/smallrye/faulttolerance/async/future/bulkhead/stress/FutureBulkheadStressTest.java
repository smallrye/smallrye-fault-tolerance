package io.smallrye.faulttolerance.async.future.bulkhead.stress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.smallrye.faulttolerance.TestArchive;

@RunWith(Arquillian.class)
public class FutureBulkheadStressTest {
    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(FutureBulkheadStressTest.class)
                .addPackage(FutureBulkheadStressTest.class.getPackage());
    }

    @Test
    public void stressTest(BulkheadService service) throws InterruptedException, ExecutionException {
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < BulkheadService.BULKHEAD_SIZE + BulkheadService.BULKHEAD_QUEUE_SIZE; i++) {
            futures.add(service.hello());
        }

        Set<String> results = new HashSet<>();
        for (Future<String> future : futures) {
            results.add(future.get());
        }

        assertEquals(BulkheadService.BULKHEAD_SIZE + BulkheadService.BULKHEAD_QUEUE_SIZE, results.size());

        for (int i = 0; i < BulkheadService.BULKHEAD_SIZE + BulkheadService.BULKHEAD_QUEUE_SIZE; i++) {
            assertTrue(results.contains("" + i));
        }
    }
}
