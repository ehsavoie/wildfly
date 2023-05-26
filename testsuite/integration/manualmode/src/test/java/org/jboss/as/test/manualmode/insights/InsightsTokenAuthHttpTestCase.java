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
import org.awaitility.Awaitility;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.mock.action.ExpectationResponseCallback;
import org.mockserver.model.*;
import org.mockserver.verify.VerificationTimes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockserver.verify.VerificationTimes.exactly;

public class InsightsTokenAuthHttpTestCase extends AbstractInsightsClientTestCase {

    private ClientAndServer mockServer;

    private static final HttpRequest CONNECT_REQUEST = HttpRequest.request()
            .withMethod("POST")
            .withPath("/api/ingress/v1/upload")
            .withHeaders(
                    Header.header("Content-Type", "multipart/form-data.*"),
                    Header.header("Authorization", "Basic " + InisghtsClientPropertiesSetup.MOCK_TOKEN)
            )
            .withBody(StringBody.subString("_connect.gz"));

    private static final HttpRequest UPDATE_REQUEST = HttpRequest.request()
            .withMethod("POST")
            .withPath("/api/ingress/v1/upload")
            .withHeaders(
                    Header.header("Content-Type", "multipart/form-data.*"),
                    Header.header("Authorization", "Basic " + InisghtsClientPropertiesSetup.MOCK_TOKEN)
            )
            .withBody(StringBody.subString("_update.gz"));

    private static final HttpResponse MOCK_RESPONSE_ACCEPTED = HttpResponse.response()
            .withStatusCode(HttpStatusCode.ACCEPTED_202.code())
            .withBody("ACCEPTED");

    @BeforeClass
    public static void setup() throws Exception {
        container.startInAdminMode();
        setupTask.setupTokenHttp(container.getClient());
        container.stop();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        container.startInAdminMode();
        setupTask.tearDown(container.getClient());
        container.stop();
    }

    @Before
    public void startMockServer() throws IOException {
        mockServer = ClientAndServer.startClientAndServer(1080);
        setupMockServerExpectations();
    }

    @After
    public void stopMockServer() {
        mockServer.stop();
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
        assertEquals("token", basicReport.get("app.transport.type.https").asText());
        assertEquals(setupTask.MOCK_TOKEN, basicReport.get("app.auth.token").asText());
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
