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
import org.apache.commons.io.input.Tailer;
import org.awaitility.Awaitility;
import org.jboss.as.controller.client.ModelControllerClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.Header;
import org.mockserver.model.HttpClassCallback;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.HttpStatusCode;
import org.mockserver.model.StringBody;
import org.mockserver.verify.VerificationTimes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.jboss.as.test.manualmode.insights.InisghtsClientPropertiesSetup.JAVA_ARCHIVE_UPLOAD_DIR;
import static org.jboss.as.test.manualmode.insights.InisghtsClientPropertiesSetup.MACHINE_ID_FILE_PATH_PROPERTY;
import static org.jboss.as.test.manualmode.insights.InsightsFileWriterClientTestCase.INSIGHTS_DEBUG_ALL_CLIENTS_FAILED;
import static org.jboss.as.test.manualmode.insights.InsightsFileWriterClientTestCase.INSIGHTS_IWE_PATTERN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.exactly;

public class InsightsHttpClientTestCase extends AbstractInsightsClientTestCase {

    public static Pattern INSIGHTS_HTTP_ERROR_PATTERN = Pattern.compile(".*DEBUG \\[org.jboss.eap.insights.report\\].*HttpHostConnectException.*");
    public static Pattern INSIGHTS_HTTP_REQUEST_TIMEOUT_ERROR = Pattern.compile(".*DEBUG \\[org.jboss.eap.insights.report\\].*HttpTimeoutException.*");
    public static Pattern INSIGHTS_CLIENT_FAILED_PATTERN = Pattern.compile(".*DEBUG \\[org.jboss.eap.insights.report\\].*I4ASR0023.*");
    public static Pattern INSIGHTS_LOG_PATTERN = Pattern.compile(".*org.jboss.eap.insights.report.*");
    public static Pattern INSIGHTS_CLIENTS_NOT_READY = Pattern.compile(".*DEBUG \\[org.jboss.eap.insights.report\\].*Insights is not configured to send: JBossInsightsConfiguration.*");

    // TODO;
    // check no server DONE
    // check various other http codes DONE not found
    // check weird stuff with binary DONE
    // check readiness checks DONE machine ID
    // proxy?
    // add log checks

    private ClientAndServer mockServer;

    private static final HttpRequest CONNECT_REQUEST = HttpRequest.request()
            .withMethod("POST")
            .withPath("/api/ingress/v1/upload")
            .withHeaders(
                    Header.header("Content-Type", "multipart/form-data.*")
            )
            .withBody(StringBody.subString("_connect.gz"));

    private static final HttpRequest UPDATE_REQUEST = HttpRequest.request()
            .withMethod("POST")
            .withPath("/api/ingress/v1/upload")
            .withHeaders(
                    Header.header("Content-Type", "multipart/form-data.*")
            )
            .withBody(StringBody.subString("_update.gz"));

    private static final HttpResponse MOCK_RESPONSE_ACCEPTED = response()
            .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
            .withBody("ACCEPTED");

    @BeforeClass
    public static void setup() throws Exception {
        container.startInAdminMode();
        setupTask.setup(container.getClient());
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
    public void startMockServer() {
        mockServer = ClientAndServer.startClientAndServer(1080);
        setupMockServerExpectations();
    }

    @After
    public void stopMockServer() {
        mockServer.stop();
    }

    @Test
    public void testServerNotResponding() throws Exception {
        mockServer.reset();
        mockServer.withSecure(true).when(CONNECT_REQUEST)
                .respond(
                        response()
                                .withBody("some_response_body")
                                .withDelay(TimeUnit.SECONDS, 10000)
                );
        container.startInAdminMode();
        final ModelControllerClient controllerClient = container.getClient().getControllerClient();
        String uploadDirOriginal = setupTask.readProperty(controllerClient, "rht.insights.java.archive.upload.dir");
        setupTask.addProperty(controllerClient, "rht.insights.java.archive.upload.dir", JAVA_ARCHIVE_UPLOAD_DIR.resolve("nonexistent").toString());
        setupTask.addProperty(controllerClient, "rht.insights.java.http.client.timeout", Duration.ofSeconds(1).toString());
        container.stop();
        InsightsLogListener listener = new InsightsLogListener(
                INSIGHTS_HTTP_ERROR_PATTERN,
                INSIGHTS_DEBUG_ALL_CLIENTS_FAILED,
                INSIGHTS_IWE_PATTERN,
                INSIGHTS_HTTP_REQUEST_TIMEOUT_ERROR);
        Tailer insightsLogTailer = null;
        try {
            insightsLogTailer = Tailer.create(serverLogFile.toFile(), listener, 200, true);
            container.start();
            Awaitility.await("Waiting for client failure.").atMost(Duration.ofSeconds(30)).untilAsserted(() -> listener.assertPatternMatched(INSIGHTS_DEBUG_ALL_CLIENTS_FAILED));
            listener.assertPatternMatched(INSIGHTS_HTTP_REQUEST_TIMEOUT_ERROR);
            Assert.assertFalse("There should be no INFO, WARN or ERROR logs from Insights.", listener.foundMatchForPattern(INSIGHTS_IWE_PATTERN));
        } finally {
            setupTask.addProperty(controllerClient, "rht.insights.java.archive.upload.dir", uploadDirOriginal);
            setupTask.removeProperty(controllerClient, "rht.insights.java.http.client.timeout");
            insightsLogTailer.stop();
            container.stop();
        }
    }

    @Test
    public void testPayloadNotAccepted() throws Exception {
        container.startInAdminMode();
        final ModelControllerClient controllerClient = container.getClient().getControllerClient();
        String uploadDirOriginal = setupTask.readProperty(controllerClient, "rht.insights.java.archive.upload.dir");
        setupTask.addProperty(controllerClient, "rht.insights.java.archive.upload.dir", JAVA_ARCHIVE_UPLOAD_DIR.resolve("nonexistent").toString());
        container.stop();
        mockServer.reset();
        InsightsLogListener listener = new InsightsLogListener(
                INSIGHTS_CLIENT_FAILED_PATTERN,
                INSIGHTS_DEBUG_ALL_CLIENTS_FAILED,
                INSIGHTS_IWE_PATTERN);
        Tailer insightsLogTailer = null;
        try {
            insightsLogTailer = Tailer.create(serverLogFile.toFile(), listener, 200, true);
            container.start();
            Awaitility.await("Waiting for client failure.").atMost(Duration.ofSeconds(30)).untilAsserted(() -> listener.assertPatternMatched(INSIGHTS_DEBUG_ALL_CLIENTS_FAILED));
            listener.assertPatternMatched(INSIGHTS_CLIENT_FAILED_PATTERN);
            Assert.assertFalse("There should be no INFO, WARN or ERROR logs from Insights.", listener.foundMatchForPattern(INSIGHTS_IWE_PATTERN));
        } finally {
            setupTask.addProperty(controllerClient, "rht.insights.java.archive.upload.dir", uploadDirOriginal);
            insightsLogTailer.stop();
            container.stop();
        }
    }

    @Test
    public void testNoBinary() throws Exception {
        container.startInAdminMode();
        final ModelControllerClient controllerClient = container.getClient().getControllerClient();
        Path unreadableFile = Files.createTempFile("unreadable", null);
        Files.setPosixFilePermissions(unreadableFile, PosixFilePermissions.fromString("---------"));
        String originalKeyFilePath = setupTask.readProperty(controllerClient, "rht.insights.java.key.file.path");
        String originalCertFilePath = setupTask.readProperty(controllerClient, "rht.insights.java.cert.file.path");
        String originalBinary = setupTask.readProperty(controllerClient, "rht.insights.java.cert.helper.binary");
        setupTask.addProperty(controllerClient, "rht.insights.java.key.file.path", unreadableFile.toString());
        setupTask.addProperty(controllerClient, "rht.insights.java.cert.file.path", unreadableFile.toString());
        setupTask.addProperty(controllerClient, "rht.insights.java.cert.helper.binary", "/nonexistent/binary/path");
        String uploadDirOriginal = setupTask.readProperty(controllerClient, "rht.insights.java.archive.upload.dir");
        setupTask.addProperty(controllerClient, "rht.insights.java.archive.upload.dir", JAVA_ARCHIVE_UPLOAD_DIR.resolve("nonexistent").toString());
        container.stop();

        InsightsLogListener listener = new InsightsLogListener(
                INSIGHTS_DEBUG_ALL_CLIENTS_FAILED,
                INSIGHTS_IWE_PATTERN);
        Tailer insightsLogTailer = null;
        try {
            insightsLogTailer = Tailer.create(serverLogFile.toFile(),listener, 200, true);
            container.start();
            Awaitility.await("Waiting for client failure.").atMost(Duration.ofSeconds(30)).untilAsserted(() -> listener.assertPatternMatched(INSIGHTS_DEBUG_ALL_CLIENTS_FAILED));
            Assert.assertFalse("There should be no INFO, WARN or ERROR logs from Insights.", listener.foundMatchForPattern(INSIGHTS_IWE_PATTERN));
        } finally {
            setupTask.addProperty(controllerClient, "rht.insights.java.key.file.path", originalKeyFilePath);
            setupTask.addProperty(controllerClient, "rht.insights.java.cert.file.path", originalCertFilePath);
            setupTask.addProperty(controllerClient, "rht.insights.java.cert.helper.binary", originalBinary);
            setupTask.addProperty(controllerClient, "rht.insights.java.archive.upload.dir", uploadDirOriginal);
            container.stop();
            insightsLogTailer.stop();
        }
    }

    @Test
    public void testNotReadyWhenMissingMachineID() throws Exception {
        container.startInAdminMode();
        final ModelControllerClient controllerClient = container.getClient().getControllerClient();
        String originalMachineID = setupTask.readProperty(controllerClient, MACHINE_ID_FILE_PATH_PROPERTY);
        setupTask.addProperty(controllerClient, MACHINE_ID_FILE_PATH_PROPERTY, "/nonexistent/machineID");
        container.stop();

        InsightsLogListener listener = new InsightsLogListener(INSIGHTS_CLIENTS_NOT_READY, INSIGHTS_LOG_PATTERN);
        Tailer insightsLogTailer = null;
        try {
            insightsLogTailer = Tailer.create(serverLogFile.toFile(),listener, 200, true);
            container.start();
            Awaitility.await("Waiting for Insights clients not ready.").atMost(Duration.ofSeconds(5)).untilAsserted(() -> listener.assertPatternMatched(INSIGHTS_CLIENTS_NOT_READY));
            assertEquals("There should be just a single debug message when clients are not ready. But there was: " + listener.getMatchedLines(), 1, listener.getMatchedLines().size());
        } finally {
            insightsLogTailer.stop();
            setupTask.addProperty(controllerClient, MACHINE_ID_FILE_PATH_PROPERTY, originalMachineID);
            container.stop();
        }
    }

    @Override
    protected void assertNoRequest() {
        mockServer.verifyZeroInteractions();
    }

    @Override
    protected void assertReady() {
        assertTrue(mockServer.isRunning());
        mockServer.verifyZeroInteractions();
    }

    @Override
    protected void awaitConnect(long seconds) {
        awaitRequest(CONNECT_REQUEST, seconds, exactly(1));
    }

    @Override
    protected void awaitUpdate(long seconds) {
        awaitRequest(UPDATE_REQUEST, seconds, exactly(1));
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
    protected void cleanRequests() {
        mockServer.reset();
        setupMockServerExpectations();
    }

    @Override
    protected void checkBasicReportClientSpecifics(JsonNode basicReport) {
        assertEquals(setupTask.getCertFilePath(), basicReport.get("app.transport.cert.https").asText());
        assertEquals("mtls", basicReport.get("app.transport.type.https").asText());
    }

    private InsightsRequest getRequest(HttpRequest request) throws Exception {
        return InsightsHttpRequestParser.parse(mockServer.retrieveRecordedRequests(request)[0]);
    }

    private void awaitRequest(HttpRequest request, long seconds, VerificationTimes times) {
        Awaitility.await().atMost(Duration.ofSeconds(seconds)).untilAsserted(() -> mockServer.verify(request, times));
    }

    private void setupMockServerExpectations() {
        mockServer.withSecure(true).when(CONNECT_REQUEST).respond(
                HttpClassCallback.callback(TestCallback.class)
        );
        mockServer.withSecure(true).when(UPDATE_REQUEST).respond(
                HttpClassCallback.callback(TestCallback.class)
        );
    }

    public static class TestCallback implements ExpectationResponseCallback {

        @Override
        public HttpResponse handle(HttpRequest httpRequest) {
            return MOCK_RESPONSE_ACCEPTED;
        }
    }
}
