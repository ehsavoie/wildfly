package org.jboss.as.test.manualmode.insights;

import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.configuration.ConfigurationProperties;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.ClearType;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.RequestDefinition;
import org.mockserver.verify.VerificationTimes;
import org.wildfly.core.testrunner.ServerControl;
import org.wildfly.core.testrunner.ServerController;

import jakarta.inject.Inject;
import org.wildfly.core.testrunner.WildflyTestRunner;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * Configure 2-way SSL and test insight can connect to server or logs correct errors.
 */
@RunWith(WildflyTestRunner.class)
@ServerControl(manual = true)
public class InsightsCertTestCase {

    private static final Logger log = Logger.getLogger(InsightsCertTestCase.class);

    private static final String LOG_FILE_HANDLER_NAME = "test";
    private static final String LOG_FILE_NAME = "insights.log";
    private static final String LOG_MOCKSERVER = "mockserver.log";
    private static final int port = 8569;

    private static Path machineIdFilePath;
    private static ClientAndServer mockClientAndServer;
    private static HttpRequest postHttpsRequest;
    private static Path serverLogFile;
    private static Path mockServerLogFile;
    private static String originalJbossArgsProperty;

    @Inject
    private static ServerController container;

    @BeforeClass
    public static void setupBeforeTestCase() throws Exception {
        // create dummy machineId file which is normally used to check that RHEL Insights client is registered
        machineIdFilePath = createMachineIdFile();
        container.startInAdminMode();
        try (CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine("/subsystem=logging/file-handler=" + LOG_FILE_HANDLER_NAME + ":add(append=false,autoflush=true,name=PATTERN,file={relative-to=jboss.server.log.dir, path="
                    + LOG_FILE_NAME + "}, level=DEBUG, formatter=\"%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n\")", true);
            cli.sendLine("/subsystem=logging/root-logger=ROOT:add-handler(name=" + LOG_FILE_HANDLER_NAME + ")", true);
            cli.sendLine("/subsystem=logging/root-logger=ROOT:write-attribute(name=level, value=DEBUG");
            cli.sendLine("/subsystem=logging/console-handler=CONSOLE:write-attribute(name=level, value=DEBUG", true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        serverLogFile = getAbsoluteLogFilePath(LOG_FILE_NAME);
        setupMockServerLogging();
        container.stop();

        originalJbossArgsProperty = System.getProperty("jboss.args");
        log.debug("Original system property: " + originalJbossArgsProperty);

        startMockServer();
    }

    @Before
    public void setup() throws Exception {
        cleanLogFiles();
        mockClientAndServer.clear(request(), ClearType.LOG);
    }

    @Test
    public void testTwoWaySSL() throws Exception {
        configureInsightsClient(getFileFromResources("insights/mockserverclientkeycerts/client-cert.pem"), getFileFromResources("insights/mockserverclientkeycerts/client-key.pem"));

        container.start();
        System.out.println("-------------------------\n" + mockClientAndServer.retrieveLogMessages(postHttpsRequest));
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() ->
                        mockClientAndServer.verify(postHttpsRequest, VerificationTimes.exactly(1))
                );

        String expectedLogMessage = "Red Hat Insights - Payload was accepted for processing";
        Assert.assertFalse("Log Message:\n\"" + expectedLogMessage + "\"\n is expected to be present in log but it's not.", searchTextInLog(expectedLogMessage, serverLogFile, Duration.ofSeconds(5)).isEmpty());

        RequestDefinition[] recordedRequests = mockClientAndServer
                .retrieveRecordedRequests(postHttpsRequest);
        InsightsRequest request = InsightsHttpRequestParser.parse((HttpRequest) recordedRequests[0]);
        Assert.assertNotNull("Payload received by MockServer(simulating Insights server) did not receive any payload or could not be parsed.",
                request.getPayload().get("basic").asText());
    }

    @Test
    public void testNonReadableCert() throws Exception {
        configureInsightsClient(createNonReadableFile(), getFileFromResources("insights/mockserverclientkeycerts/client-key.pem"));
        container.start();

        String expectedLogMessage = ".*DEBUG.*insights.*java\\.nio\\.file\\.AccessDeniedException.*";
        Assert.assertFalse("Log Message:\n\"" + expectedLogMessage + "\"\n is expected to be present in log but it's not.", searchRegexInLog(expectedLogMessage, serverLogFile, Duration.ofSeconds(15)).isEmpty());

        String unexpectedLogMessage = ".*(?:INFO|WARN|ERROR).*insights.*";
        List<String> logMessages = searchRegexInLog(unexpectedLogMessage, serverLogFile);
        Assert.assertTrue("Log Messages:\n\"" + logMessages + "\"\n are not expected to be present in log but it is there.", logMessages.isEmpty());
    }

    @Test
    public void testNonReadableKey() throws Exception {
        configureInsightsClient(getFileFromResources("insights/mockserverclientkeycerts/client-cert.pem"), createNonReadableFile());
        container.start();

        String expectedLogMessage = ".*DEBUG.*insights.*java\\.nio\\.file\\.AccessDeniedException.*";
        Assert.assertFalse("Log Message:\n\"" + expectedLogMessage + "\"\n is expected to be present in log but it's not.", searchRegexInLog(expectedLogMessage, serverLogFile, Duration.ofSeconds(15)).isEmpty());

        String unexpectedLogMessage = ".*(?:INFO|WARN|ERROR).*insights.*";
        List<String> logMessages = searchRegexInLog(unexpectedLogMessage, serverLogFile);
        Assert.assertTrue("Log Messages:\n\"" + logMessages + "\"\n are not expected to be present in log but it is there.", logMessages.isEmpty());

    }

    @Test
    public void testBadKeyLocation() throws Exception {
        configureInsightsClient(getFileFromResources("insights/mockserverclientkeycerts/client-cert.pem"), "non-existent-file.txt");
        container.start();

        String unexpectedLogMessage = ".*(?:INFO|WARN|ERROR).*insights.*";
        List<String> logMessages = searchRegexInLog(unexpectedLogMessage, serverLogFile);
        Assert.assertTrue("Log Messages:\n\"" + logMessages + "\"\n are not expected to be present in log but it is there.", logMessages.isEmpty());
    }

    @Test
    public void testBadCertLocation() throws Exception {
        configureInsightsClient("non-existent-file.txt", getFileFromResources("insights/mockserverclientkeycerts/client-key.pem"));

        container.start();

        String unexpectedLogMessage = ".*(?:INFO|WARN|ERROR).*insights.*";
        List<String> logMessages = searchRegexInLog(unexpectedLogMessage, serverLogFile);
        Assert.assertTrue("Log Messages:\n\"" + logMessages + "\"\n are not expected to be present in log but it is there.", logMessages.isEmpty());
    }

    @Test
    public void testExpiredCert() throws Exception {
        configureInsightsClient(getFileFromResources("insights/mockserverclientkeycerts/client-expired-cert.pem"), getFileFromResources("insights/mockserverclientkeycerts/client-key.pem"));
        container.start();

        String expectedLogMessage1 = "java.security.cert.CertificateExpiredException";
        Assert.assertFalse("Log Message:\n\"" + expectedLogMessage1 + "\"\n is expected to be present in log but it's not.", searchTextInLog(expectedLogMessage1, mockServerLogFile, Duration.ofSeconds(15)).isEmpty());

        String unexpectedLogMessage = ".*(?:INFO|WARN|ERROR).*insights.*";
        List<String> logMessages = searchRegexInLog(unexpectedLogMessage, serverLogFile);
        Assert.assertTrue("Log Messages:\n\"" + logMessages + "\"\n are not expected to be present in log but it is there.", logMessages.isEmpty());
    }

    @Test
    public void testNonExistentUploadUrl() throws Exception {
        configureInsightsClient(getFileFromResources("insights/mockserverclientkeycerts/client-cert.pem"), getFileFromResources("insights/mockserverclientkeycerts/client-key.pem"), "https://example.invalid:" + port);
        container.start();

        String expectedLogMessage = ".*DEBUG.*java\\.net\\.UnknownHostException.*";
        Assert.assertFalse("Log Message:\n\"" + expectedLogMessage + "\"\n is expected to be present in log but it's not.", searchRegexInLog(expectedLogMessage, serverLogFile, Duration.ofSeconds(15)).isEmpty());

        String unexpectedLogMessage = ".*(?:INFO|WARN|ERROR).*insights.*";
        List<String> logMessages = searchRegexInLog(unexpectedLogMessage, serverLogFile);
        Assert.assertTrue("Log Messages:\n\"" + logMessages + "\"\n are not expected to be present in log but it is there.", logMessages.isEmpty());
    }

    @Test
    public void testNoErrorIfNoClientIsReadyToSent() throws Exception {
        Assume.assumeFalse("Certificate or key is present in /etc/pki/consumer directory. Skipping test.", new File("/etc/pki/consumer/key.pem").exists() || new File("/etc/pki/consumer/cert.pem").exists());

        configureInsightsClient(null, null, null);
        container.start();

        String unexpectedLogMessage = ".*(?:INFO|WARN|ERROR).*insights.*";
        List<String> logMessages = searchRegexInLog(unexpectedLogMessage, serverLogFile);
        Assert.assertTrue("Log Messages:\n\"" + logMessages + "\"\n are not expected to be present in log but it is there.", logMessages.isEmpty());
    }

    @After
    public void tearDown() {
        System.setProperty("jboss.args", originalJbossArgsProperty);
        container.stop();
    }

    @AfterClass
    public static void cleanEnvAfterTestCase() throws Exception {
        stopMockServer();
        container.startInAdminMode();
        try (CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine(" /subsystem=logging/root-logger=ROOT:remove-handler(name=" + LOG_FILE_HANDLER_NAME + ")", true);
            cli.sendLine("/subsystem=logging/file-handler=" + LOG_FILE_HANDLER_NAME + ":remove()", true);
            cli.sendLine("/subsystem=logging/root-logger=ROOT:write-attribute(name=level, value=INFO", true);
            cli.sendLine("/subsystem=logging/console-handler=CONSOLE:write-attribute(name=level, value=INFO", true);
        }
        container.stop();
        System.setProperty("jboss.args", originalJbossArgsProperty);
        restoreTestLogging();
        Files.deleteIfExists(machineIdFilePath);
    }

    private static void startMockServer() throws IOException {
        // log MockServer to file
        ConfigurationProperties.logLevel("TRACE");
        ConfigurationProperties.tlsMutualAuthenticationRequired(true);
        ConfigurationProperties.privateKeyPath(getFileFromResources("insights/mockserverclientkeycerts/server-key.pem"));
        ConfigurationProperties.x509CertificatePath(getFileFromResources("insights/mockserverclientkeycerts/server-cert.pem"));
        ConfigurationProperties.certificateAuthorityCertificate(getFileFromResources("insights/mockserverclientkeycerts/ca.pem"));
        ConfigurationProperties.certificateAuthorityPrivateKey(getFileFromResources("insights/mockserverclientkeycerts/ca-key.pem"));
        ConfigurationProperties.tlsMutualAuthenticationCertificateChain(getFileFromResources("insights/mockserverclientkeycerts/ca.pem"));

        mockClientAndServer = startClientAndServer(port);
        mockClientAndServer.withSecure(true);

        postHttpsRequest = request()
                .withMethod("POST")
                .withSecure(true)
                .withPath("/api/ingress/v1/upload");

        mockClientAndServer
                .when(postHttpsRequest)
                .respond(
                        response()
                                .withBody("\"request_id\":\"103f2372045844c4a09a68fa67ed8ad5\",\"upload\":{\"account_number\":\"6430504\",\"org_id\":\"13510426\"}")
                                .withHeader("Content-Type", "application/json")
                                .withStatusCode(202)
                );

        mockClientAndServer
                .when(request()
                        .withMethod("POST")
                        .withPath("/api/ingress/v1/upload"))
                .respond(
                        response()
                                .withStatusCode(301)
                );

        log.debug("Mock server started.");
    }

    private static void setupMockServerLogging() throws IOException {
        mockServerLogFile = getAbsoluteLogFilePath(LOG_MOCKSERVER);
        mockServerLogFile.toFile().delete();
        mockServerLogFile.toFile().createNewFile();
        String mockServerFileHandlerName = "mockserver";
        // as MockServer will load logging.properties file from resources, we need to adjust it by "mockserver" FileHandler for this test
        String loggingConfiguration = Files.lines(Paths.get(getFileFromResources("logging.properties"))).map(line -> line.contains("logger.handlers=") ? line.concat("," + mockServerFileHandlerName) : line)
                .collect(Collectors.joining("\n")) +
                "# File handler configuration\n" +
                "handler." + mockServerFileHandlerName + "=org.jboss.logmanager.handlers.FileHandler\n" +
                "handler." + mockServerFileHandlerName + ".properties=autoFlush,fileName\n" +
                "handler." + mockServerFileHandlerName + ".autoFlush=true\n" +
                "handler." + mockServerFileHandlerName + ".fileName=" + mockServerLogFile.toAbsolutePath() + "\n" +
                "handler." + mockServerFileHandlerName + ".formatter=PATTERN\n";
        java.util.logging.LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(loggingConfiguration.getBytes(UTF_8)));
    }

    private static void restoreTestLogging() throws IOException {
        java.util.logging.LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(getFileFromResources("logging.properties").getBytes(UTF_8)));
    }

    private static void stopMockServer() {
        if (mockClientAndServer != null) {
            mockClientAndServer.stop(true);
            if (!mockClientAndServer.hasStopped(3, 2, TimeUnit.SECONDS)) {
                throw new RuntimeException("Mock server failed to stop.");
            }
        }
    }

    private static String createNonReadableFile() throws IOException {
        Path nonReadableCert = Files.createTempFile(null, null);
        Assert.assertTrue("It's not possible to set file non-readable: " + nonReadableCert, nonReadableCert.toFile().setReadable(false));
        return nonReadableCert.toString();
    }

    private static Path createMachineIdFile() throws IOException {
        return Files.createTempFile(null, null);
    }

    private void configureInsightsClient(String certFilePath, String keyFilePath) {
        configureInsightsClient(certFilePath, keyFilePath, "https://127.0.0.1:" + port);
    }

    private void configureInsightsClient(String certFilePath, String keyFilePath, String uploadUrl) {
        StringBuilder str = new StringBuilder(originalJbossArgsProperty);
        if (StringUtils.isNotEmpty(certFilePath)) {
            str.append(" -Drht.insights.java.cert.file.path=").append(certFilePath);
        }
        if (StringUtils.isNotEmpty(keyFilePath)) {
            str.append(" -Drht.insights.java.key.file.path=").append(keyFilePath);
        }

        if (StringUtils.isNotEmpty(uploadUrl)) {
            str.append(" -Drht.insights.java.upload.base.url=").append(uploadUrl);
        }

        // set machineId file path as no client will work without it
        str.append(" -Dunsupported.machine.id.file.path=").append(machineIdFilePath.toString());

        // set custom truststore with our custom CA certificate to server (it will be used by integrated Insights client for TLS authentication to MockServer)
        str.append(" -Djavax.net.ssl.trustStore=" + getFileFromResources("insights/mockserverclientkeycerts/truststore.p12"));
        str.append(" -Djavax.net.ssl.trustStorePassword=changeit");

        System.setProperty("jboss.args", str.toString());
        log.debug("Set system property jboss.args=" + str);
    }

    private static Path getAbsoluteLogFilePath(final String filename) {
        return Paths.get(resolveRelativePath("jboss.server.log.dir"), filename);
    }

    private static String resolveRelativePath(final String relativePath) {
        final ModelNode address = PathAddress.pathAddress(
                PathElement.pathElement(ModelDescriptionConstants.PATH, relativePath)
        ).toModelNode();
        final ModelNode result;
        try {
            final ModelNode op = Operations.createReadAttributeOperation(address, ModelDescriptionConstants.PATH);
            result = container.getClient().getControllerClient().execute(op);
            if (Operations.isSuccessfulOutcome(result)) {
                return Operations.readResult(result).asString();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException(Operations.getFailureDescription(result).asString());
    }

    private List<String> searchTextInLog(final String msg, Path logFile, Duration timeout) throws IOException {
        return searchInLog((line) -> line.contains(msg), logFile, timeout);
    }

    private List<String> searchRegexInLog(final String regex, Path logFile) throws IOException {
        return searchInLog((line) -> Pattern.matches(regex, line), logFile);
    }

    private List<String> searchRegexInLog(final String regex, Path logFile, Duration timeout) throws IOException {
        return searchInLog((line) -> Pattern.matches(regex, line), logFile, timeout);
    }

    private List<String> searchInLog(Predicate<String> predicate, Path logFile) throws IOException {
        try (final BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
            return reader.lines().filter(predicate).collect(Collectors.toList());
        }
    }

    private List<String> searchInLog(Predicate<String> predicate, Path logFile, Duration timeout) throws IOException {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - timeout.toMillis() < startTime) {
            List<String> foundLogs = searchInLog(predicate, logFile);
            if (!foundLogs.isEmpty()) {
                return foundLogs;
            }
        }
        return new ArrayList<>();
    }

    private void cleanLogFiles() throws Exception {
        serverLogFile.toFile().delete();
        serverLogFile.toFile().createNewFile();
    }

    private static String getFileFromResources(String path) {
        return InsightsCertTestCase.class
                .getClassLoader()
                .getResource(path)
                .getPath();
    }
}
