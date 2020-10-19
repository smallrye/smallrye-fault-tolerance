package io.smallrye.faulttolerance.async.sizing;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.runner.RunWith;

import io.smallrye.faulttolerance.TestArchive;

@RunWith(Arquillian.class)
public class AsyncThreadPoolSizingOldConfigTest extends AbstractAsyncThreadPoolSizingTest {
    private static final String CONFIG = ""
            + "io.smallrye.faulttolerance.globalThreadPoolSize=" + SIZE + "\n"
            + "io.smallrye.faulttolerance.mainThreadPoolQueueSize=0\n";

    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(AsyncThreadPoolSizingOldConfigTest.class)
                .addPackage(AsyncThreadPoolSizingOldConfigTest.class.getPackage())
                .addAsManifestResource(new StringAsset(CONFIG), "microprofile-config.properties");
    }
}
