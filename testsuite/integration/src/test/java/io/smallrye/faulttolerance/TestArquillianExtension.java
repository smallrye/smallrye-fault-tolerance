package io.smallrye.faulttolerance;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.container.ClassContainer;

public class TestArquillianExtension implements LoadableExtension {
    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(ApplicationArchiveProcessor.class, TestApplicationArchiveProcessor.class);
    }

    public static class TestApplicationArchiveProcessor implements ApplicationArchiveProcessor {
        @Override
        public void process(Archive<?> applicationArchive, TestClass testClass) {
            if (applicationArchive instanceof ClassContainer) {
                ClassContainer<?> classContainer = (ClassContainer<?>) applicationArchive;
                classContainer.addClass(TestAsyncExecutorProvider.class);
            }
        }
    }
}
