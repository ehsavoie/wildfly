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

import org.apache.commons.io.FileUtils;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.dmr.ModelNode;
import org.wildfly.core.testrunner.ManagementClient;
import org.wildfly.core.testrunner.ServerSetupTask;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import static org.jboss.as.test.manualmode.insights.AbstractInsightsClientTestCase.CLIENT_MAX_RETRY;

public class InisghtsClientPropertiesSetup implements ServerSetupTask {

    private static final String INSIGHTS_CONSOLE_DEBUG_PROPERTY = "test.insights.console.debug";
    public static final String MACHINE_ID_FILE_PATH_PROPERTY = "unsupported.machine.id.file.path";
    public static Path JAVA_ARCHIVE_UPLOAD_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "insights-runtimes", "uploads");
    public static final String LOG_FILE_NAME = "insights.log";
    private static final String LOG_DIR_PROPERTY_PATH = "path=jboss.server.log.dir";
    public static final String MOCK_TOKEN = "amRvZTp2UUpBOXdJb3BxM1VhbkpJ";
    public static final String MOCK_URL = "http://localhost:1080";

    int serverPort;
    private String certFilePath = getClass().getResource("dummy.cert").getPath();
    private String keyFilePath = getClass().getResource("dummy.key").getPath();

    public InisghtsClientPropertiesSetup(int serverPort) {
        this.serverPort = serverPort;
    }

    @Override
    public void setup(ManagementClient managementClient) throws Exception {
        final ModelControllerClient controllerClient = managementClient.getControllerClient();
        addProperty(controllerClient, "rht.insights.java.upload.base.url", MOCK_URL);
        addProperty(controllerClient, CLIENT_MAX_RETRY, "1");
        setupCertAndKeyProperties(controllerClient);

        setupInsightsLogger(controllerClient);
    }

    public void setupFileWriter(ManagementClient managementClient) throws Exception {
        // TODO createTempDirectory?
        Files.createDirectories(JAVA_ARCHIVE_UPLOAD_DIR);
        final ModelControllerClient controllerClient = managementClient.getControllerClient();
        addProperty(controllerClient, "rht.insights.java.archive.upload.dir", JAVA_ARCHIVE_UPLOAD_DIR.toString());
        addProperty(controllerClient, CLIENT_MAX_RETRY, "1");
        setupCertAndKeyProperties(controllerClient);
        setupInsightsLogger(controllerClient);
    }

    public  void setupTokenHttp(ManagementClient managementClient) throws Exception {
        final ModelControllerClient controllerClient = managementClient.getControllerClient();
        addProperty(controllerClient, CLIENT_MAX_RETRY, "1");
        addProperty(controllerClient, "rht.insights.java.upload.base.url", System.getProperty("test.insights.java.upload.base.url", MOCK_URL));
        addProperty(controllerClient,"rht.insights.java.auth.token", System.getProperty("test.insights.java.auth.token", MOCK_TOKEN));

        setupInsightsLogger(controllerClient);
    }

    private  void setupInsightsLogger(ModelControllerClient controllerClient) throws Exception {
        if (Boolean.getBoolean(INSIGHTS_CONSOLE_DEBUG_PROPERTY)) {
            ModelNode operation = createOpNode("subsystem=logging/console-handler=CONSOLE", ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
            operation.get("name").set("level");
            operation.get("value").set("DEBUG");
            CoreUtils.applyUpdate(operation, controllerClient);

            operation = createOpNode("subsystem=logging/logger=org.jboss.eap.insights", ModelDescriptionConstants.ADD);
            operation.get("category").set("org.jboss.eap.insights");
            operation.get("level").set("DEBUG");
            CoreUtils.applyUpdate(operation, controllerClient);
        }
        setupInsightsLogFile();
    }

    private void setupInsightsLogFile() throws Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine("/subsystem=logging/file-handler=" + "test" + ":add(append=false,autoflush=true,name=PATTERN,file={relative-to=jboss.server.log.dir, path="
                    + "insights.log" + "}, level=DEBUG, formatter=\"%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c] (%t) %s%e%n\")", false);
            cli.sendLine("/subsystem=logging/root-logger=ROOT:add-handler(name=" + "test" + ")", true);
            cli.sendLine("/subsystem=logging/root-logger=ROOT:write-attribute(name=level, value=DEBUG");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setupCertAndKeyProperties(ModelControllerClient controllerClient) throws Exception {
        String machineIdFilePath = File.createTempFile("machine-id", null).getPath();
        addProperty(controllerClient, "rht.insights.java.key.file.path", keyFilePath);
        addProperty(controllerClient, "rht.insights.java.cert.file.path", certFilePath);
        addProperty(controllerClient, MACHINE_ID_FILE_PATH_PROPERTY, machineIdFilePath);
    }

    private void tearDownInsightsLogFile() throws Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            cli.sendLine(" /subsystem=logging/root-logger=ROOT:remove-handler(name=" + "test" + ")", true);
            cli.sendLine("/subsystem=logging/file-handler=" + "test" + ":remove()", true);
            cli.sendLine("/subsystem=logging/root-logger=ROOT:write-attribute(name=level, value=INFO", true);
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient) throws Exception {
        final ModelControllerClient controllerClient = managementClient.getControllerClient();

        ModelNode operation = createOpNode("subsystem=logging/console-handler=CONSOLE", ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        operation.get("name").set("level");
        operation.get("value").set("INFO");
        CoreUtils.applyUpdate(operation, controllerClient);
        operation = createOpNode("subsystem=logging/logger=org.jboss.eap.insights", ModelDescriptionConstants.REMOVE);
        try {
            CoreUtils.applyUpdate(operation, controllerClient);
        } catch (RuntimeException ex) {
            if (!ex.getMessage().contains("WFLYCTL0216")) {
                throw new RuntimeException(ex);
            }
        }

        tearDownInsightsLogFile();

        removeProperty(controllerClient, "rht.insights.java.upload.base.url");
        removeProperty(controllerClient, "rht.insights.java.key.file.path");
        removeProperty(controllerClient, "rht.insights.java.cert.file.path");
        removeProperty(controllerClient, "rht.insights.java.archive.upload.dir");
        removeProperty(controllerClient, "rht.insights.java.auth.token");
        removeProperty(controllerClient, MACHINE_ID_FILE_PATH_PROPERTY);

        if (Files.exists(JAVA_ARCHIVE_UPLOAD_DIR)) {
            FileUtils.deleteDirectory(JAVA_ARCHIVE_UPLOAD_DIR.toFile());
        }
    }

    protected static void addProperty(ModelControllerClient client, String name, String value) throws Exception {
        removeProperty(client, name);
        if (value != null && !value.isEmpty()) {
            ModelNode operation = createOpNode("system-property=" + name, ModelDescriptionConstants.ADD);
            operation.get("value").set(value);
            CoreUtils.applyUpdate(operation, client);
        }
    }

    protected static void removeProperty(ModelControllerClient client, String name) throws Exception {
        ModelNode operation = createOpNode("system-property=" + name, ModelDescriptionConstants.REMOVE);
        try {
            CoreUtils.applyUpdate(operation, client);
        } catch (RuntimeException ex) {
            // resource not found is OK here
            if (!ex.getMessage().contains("WFLYCTL0216")) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static String readProperty(ModelControllerClient client, String name) throws Exception {
        ModelNode operation = createOpNode("system-property=" + name, ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION);
        operation.get("name").set("value");
        return getResultAsString(client.execute(operation));
    }

    public static Path getLogFilePath(ManagementClient managementClient) throws Exception {
        final ModelControllerClient controllerClient = managementClient.getControllerClient();

        ModelNode op  = createOpNode(LOG_DIR_PROPERTY_PATH, ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION);
        op.get("name").set("path");
        return Paths.get(getResultAsString(controllerClient.execute(op)), LOG_FILE_NAME);
    }

    private static String getResultAsString(ModelNode result) {
        if (result.hasDefined(ModelDescriptionConstants.OUTCOME) && result.get(ModelDescriptionConstants.OUTCOME).asString().equals(ModelDescriptionConstants.SUCCESS)) {
            return result.get(ModelDescriptionConstants.RESULT).asString();
        } else if (result.hasDefined(ModelDescriptionConstants.FAILURE_DESCRIPTION)) {
            if (result.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).asString().contains("WFLYCTL0216")) {
                return null;
            }
            throw new RuntimeException(result.get(ModelDescriptionConstants.FAILURE_DESCRIPTION).asString());
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get(ModelDescriptionConstants.OUTCOME));
        }
    }

    public String getCertFilePath() {
        return certFilePath;
    }
}
