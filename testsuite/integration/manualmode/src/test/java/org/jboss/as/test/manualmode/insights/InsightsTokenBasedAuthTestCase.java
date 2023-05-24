/*
 * Copyright 2023 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.manualmode.insights;

import org.apache.commons.io.input.Tailer;
import org.awaitility.Awaitility;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.*;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.ServerControl;
import org.wildfly.core.testrunner.ServerController;

import jakarta.inject.Inject;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Pattern;

@RunWith(Arquillian.class)
@ServerControl(manual = true)
@RunAsClient
public class InsightsTokenBasedAuthTestCase {

    @Inject
    protected static ServerController container;

    private static InisghtsClientPropertiesSetup setupTask;
    private static boolean skipped = true;
    private static Path serverLogFile;

    @BeforeClass
    public static void setupTokenAuth() throws Exception {
        Assume.assumeNotNull(
                System.getProperty("test.insights.java.auth.token"),
                System.getProperty("test.insights.java.upload.base.url")
        );
        skipped = false;
        container.startInAdminMode();
        setupTask = new InisghtsClientPropertiesSetup(1080);
        setupTask.setupTokenHttp(container.getClient());
        serverLogFile = setupTask.getLogFilePath(container.getClient());
        container.stop();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (skipped) return;

        container.startInAdminMode();
        setupTask.tearDown(container.getClient());
        container.stop();
    }

    @Test
    public void testNoErrors() throws Exception {
        Tailer insightsLogTailer = null;
        InsightsLogListener listener = new InsightsLogListener(
                Pattern.compile(".*Red Hat Insights - Payload was accepted for processing.*"),
                Pattern.compile(".*\"transport.type.https\" : \"token\".*"));
        try {
            container.start();
            insightsLogTailer = Tailer.create(serverLogFile.toFile(), listener);
            Awaitility.await("Waiting for token auth log messages.").atMost(Duration.ofSeconds(20)).untilAsserted(() -> listener.assertAllPatternsMatched());
        } finally {
            insightsLogTailer.stop();
        }
        container.stop();
    }
}
