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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.input.Tailer;
import org.awaitility.Awaitility;
import org.jboss.as.controller.client.ModelControllerClient;
import org.junit.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.jboss.as.test.manualmode.insights.InisghtsClientPropertiesSetup.JAVA_ARCHIVE_UPLOAD_DIR;
import static org.junit.Assert.*;

public class InsightsFileWriterClientTestCase extends AbstractInsightsClientTestCase {

    // TODO:
    // add log checks

    private static final String CONNECT_REQUEST = "_connect.json";
    private static final String UPDATE_REQUEST = "_update.json";

    // I4ASR0019 - all clients failed
    public static Pattern INSIGHTS_DEBUG_ALL_CLIENTS_FAILED = Pattern.compile(".*DEBUG \\[org.jboss.eap.insights.report\\].*I4ASR0019.*InsightsJdkHttpClient.*InsightsFileWritingClient.*");
    public static Pattern INSIGHTS_IWE_PATTERN = Pattern.compile(".*(INFO|WARN|ERROR) \\[org.jboss.eap.insights.report\\].*");

    @BeforeClass
    public static void setup() throws Exception {
        container.startInAdminMode();
        setupTask.setupFileWriter(container.getClient());
        serverLogFile = setupTask.getLogFilePath(container.getClient());
        container.stop();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        container.startInAdminMode();
        setupTask.tearDown(container.getClient());
        container.stop();
    }

    @Before
    public void cleanUploadDir() throws Exception {
        FileUtils.cleanDirectory(JAVA_ARCHIVE_UPLOAD_DIR.toFile());
    }

    @Test
    public void testMissingDirNoError() throws Exception {
        container.startInAdminMode();
        final ModelControllerClient controllerClient = container.getClient().getControllerClient();
        String uploadDirOriginal = setupTask.readProperty(controllerClient, "rht.insights.java.archive.upload.dir");
        setupTask.addProperty(controllerClient, "rht.insights.java.archive.upload.dir", JAVA_ARCHIVE_UPLOAD_DIR.resolve("nonexistent").toString());
        container.stop();

        Tailer insightsLogTailer = null;
        InsightsLogListener listener = new InsightsLogListener(
                INSIGHTS_DEBUG_ALL_CLIENTS_FAILED,
                INSIGHTS_IWE_PATTERN);
        try {
            insightsLogTailer = Tailer.create(serverLogFile.toFile(), listener, 200, true);
            container.start();
            Awaitility.await("Waiting for client failure.").atMost(Duration.ofSeconds(30)).untilAsserted(() -> listener.assertPatternMatched(INSIGHTS_DEBUG_ALL_CLIENTS_FAILED));
            Assert.assertFalse("There should be no INFO, WARN or ERROR logs from Insights.", listener.foundMatchForPattern(INSIGHTS_IWE_PATTERN));
        } finally {
            setupTask.addProperty(controllerClient, "rht.insights.java.archive.upload.dir", uploadDirOriginal);
            insightsLogTailer.stop();
            container.stop();
        }
    }

    @Override
    public void assertNoRequest() throws Exception {
        assertTrue(Files.isDirectory(JAVA_ARCHIVE_UPLOAD_DIR));
        assertFalse(Files.list(JAVA_ARCHIVE_UPLOAD_DIR).findAny().isPresent());
    }

    @Override
    protected void assertReady() throws Exception {
        assertNoRequest();
    }

    @Override
    protected void awaitConnect(long seconds) {
        awaitRequest(CONNECT_REQUEST, seconds, 1);
    }

    @Override
    protected void awaitUpdate(long seconds) {
        awaitRequest(UPDATE_REQUEST, seconds, 1);
    }

    @Override
    protected InsightsRequest getConnect() throws Exception {
        return getRequest(CONNECT_REQUEST);
    }

    @Override
    protected InsightsRequest getUpdate() throws Exception {
        return getRequest(UPDATE_REQUEST);
    }

    @Override
    protected void cleanRequests() throws Exception {
        cleanUploadDir();
    }

    @Override
    protected void checkBasicReportClientSpecifics(JsonNode basicReport) {
        // should also contain http client fields for troubleshooting
        assertEquals(setupTask.getCertFilePath(), basicReport.get("app.transport.cert.https").asText());
        assertFieldDefined(basicReport, "basic report", "app.transport.type.https");
        assertEquals("rhel", basicReport.get("app.transport.type.file").asText());
    }

    private void awaitRequest(String requestType, long seconds, int times) {
        Awaitility.await().atMost(Duration.ofSeconds(seconds)).untilAsserted(() -> assertFileRequest(requestType, times));
    }

    private void assertFileRequest(String requestType, int times) throws Exception {
        assertEquals(times, Files.list(JAVA_ARCHIVE_UPLOAD_DIR).filter(file -> file.toString().endsWith(requestType)).count());
    }

    private InsightsRequest getRequest(String requestType) throws Exception {
        Optional<Path> requestPathOptional = Files.list(JAVA_ARCHIVE_UPLOAD_DIR).filter(file -> file.toString().endsWith(requestType)).findFirst();
        assertTrue(requestPathOptional.isPresent());
        Path requestPath = requestPathOptional.get();
        JsonNode payload;
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = Files.newInputStream(requestPath)) {
            payload = mapper.readTree(is);
        }
        return new InsightsRequest(requestPath.getFileName().toString(), payload);
    }

}
