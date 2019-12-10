/*
 * Copyright 2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.faulttolerance.tck;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Logger;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Node;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.container.ClassContainer;
import org.jboss.shrinkwrap.api.container.LibraryContainer;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.impl.base.io.IOUtil;

/**
 *
 * @author Martin Kouba
 */
public class FaultToleranceApplicationArchiveProcessor implements ApplicationArchiveProcessor {

    private static final Logger LOGGER = Logger.getLogger(FaultToleranceApplicationArchiveProcessor.class.getName());

    private static final String MAX_THREADS_OVERRIDE = "io.smallrye.faulttolerance.globalThreadPoolSize=1000";
    private static final String MP_CONFIG_PATH = "/WEB-INF/classes/META-INF/microprofile-config.properties";

    @Override
    public void process(Archive<?> applicationArchive, TestClass testClass) {
        if (!(applicationArchive instanceof ClassContainer)) {
            LOGGER.warning(
                    "Unable to add additional classes - not a class/resource container: "
                            + applicationArchive);
            return;
        }
        ClassContainer<?> classContainer = (ClassContainer<?>) applicationArchive;

        if (applicationArchive instanceof LibraryContainer) {
            JavaArchive additionalBeanArchive = ShrinkWrap.create(JavaArchive.class);
            additionalBeanArchive.addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
            ((LibraryContainer<?>) applicationArchive).addAsLibrary(additionalBeanArchive);
        } else {
            classContainer.addAsResource(EmptyAsset.INSTANCE, "META-INF/beans.xml");
        }

        if (!applicationArchive.contains("META-INF/beans.xml")) {
            applicationArchive.add(EmptyAsset.INSTANCE, "META-INF/beans.xml");
        }

        String config;
        if (!applicationArchive.contains(MP_CONFIG_PATH)) {
            config = MAX_THREADS_OVERRIDE;
        } else {
            ByteArrayOutputStream output = readCurrentConfig(applicationArchive);
            applicationArchive.delete(MP_CONFIG_PATH);
            config = output.toString() + "\n" + MAX_THREADS_OVERRIDE;
        }
        classContainer.addAsResource(new StringAsset(config), MP_CONFIG_PATH);

        LOGGER.info("Added additional resources to " + applicationArchive.toString(true));
    }

    private ByteArrayOutputStream readCurrentConfig(Archive<?> appArchive) {
        try {
            Node node = appArchive.get(MP_CONFIG_PATH);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            IOUtil.copy(node.getAsset().openStream(), outputStream);
            return outputStream;
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare microprofile-config.properties");
        }
    }
}
