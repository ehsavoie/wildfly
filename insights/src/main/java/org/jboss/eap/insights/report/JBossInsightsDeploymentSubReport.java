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
package org.jboss.eap.insights.report;

import static com.redhat.insights.jars.JarAnalyzer.SHA512_CHECKSUM_KEY;

import com.redhat.insights.jars.RecursiveJarAnalyzerHelper;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HASH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static com.redhat.insights.jars.RecursiveJarAnalyzerHelper.createTempDirectory;
import static com.redhat.insights.jars.RecursiveJarAnalyzerHelper.isArchive;
import static com.redhat.insights.jars.RecursiveJarAnalyzerHelper.listingRequired;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.redhat.insights.InsightsSubreport;
import com.redhat.insights.jars.JarAnalyzer;
import com.redhat.insights.jars.JarInfo;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.repository.ContentRepository;
import org.jboss.dmr.ModelNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import org.jboss.as.controller.HashUtil;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNTIME_NAME;
import org.jboss.eap.insights.report.logging.InsightsReportLogger;
import org.jboss.eap.insights.report.logging.JbossLoggingInsightsLogger;

/**
 * @author Emmanuel Hugonnet (c) 2023 Red Hat, Inc.
 */
class JBossInsightsDeploymentSubReport implements InsightsSubreport {

    private final ModelControllerClient modelControllerClient;
    private final ContentRepository contentRepository;
    private final JarAnalyzer analyzer;
    private final Path tempBaseDir;
    private final RecursiveJarAnalyzerHelper recursiveJarAnalyzerHelper;
    private final Map<String, List<JarInfo>> report = new HashMap<>();
    private final Map<String, String> processedDeployment = new HashMap<>();
    private final Map<String, String> processedJar = new HashMap<>();
    private final BlockingQueue<JarInfo> archivesToSend;

    JBossInsightsDeploymentSubReport(final ModelControllerClientFactory clientFactory, final Executor executor, final ContentRepository contentRepository, final Path tempBaseDir, final BlockingQueue<JarInfo> archivesToSend) {
        this.modelControllerClient = clientFactory.createSuperUserClient(executor, false);
        this.contentRepository = contentRepository;
        this.tempBaseDir = tempBaseDir;
        this.analyzer = new JarAnalyzer(JbossLoggingInsightsLogger.INSTANCE, true);
        this.recursiveJarAnalyzerHelper = new RecursiveJarAnalyzerHelper(JbossLoggingInsightsLogger.INSTANCE);
        this.archivesToSend = archivesToSend;
    }

    @Override
    public synchronized void generateReport() {
        try {
            ModelNode op = Operations.createReadResourceOperation(PathAddress.pathAddress("deployment", "*").toModelNode(), false);
            op.get(OPERATION_HEADERS).get(ROLES).add("Monitor");
            op.get("include-runtime").set(true);
            op.get("include-defaults").set(true);
            ModelNode response = this.modelControllerClient.execute(op);
            processedDeployment.clear();
            processedJar.clear();
            report.clear();
            if (SUCCESS.equals(response.get(OUTCOME).asString())) {
                for (ModelNode deploymentResult : response.get(RESULT).asList()) {
                    if (SUCCESS.equals(deploymentResult.get(OUTCOME).asString())) {
                        ModelNode deployment = deploymentResult.get(RESULT);
                        String deploymentName = deployment.get(NAME).asString();
                        InsightsReportLogger.ROOT_LOGGER.startProcessingDeployment(deploymentName);
                        ModelNode content = deployment.get(CONTENT).asList().get(0);
                        if (content.hasDefined(HASH)) {
                            byte[] hashBytes = content.get(HASH).asBytes();
                            String hash = HashUtil.bytesToHexString(hashBytes);
                            InsightsReportLogger.ROOT_LOGGER.deploymentHashFound(deploymentName, hash);
                            Path contentPath = contentRepository.getContent(hashBytes).getPhysicalFile().toPath();
                            if (isArchive(contentPath)) {
                                Optional<JarInfo> jarInfo = analyzer.process(deploymentName, contentPath.toUri().toURL());
                                if (jarInfo.isPresent()) {
                                    JarInfo deploymentJarInfo = jarInfo.get();
                                    deploymentJarInfo.attributes().put("deployment", deploymentName);
                                    processedDeployment.put(deploymentName, hash);
                                    List<JarInfo> infos = new ArrayList<>();
                                    infos.add(deploymentJarInfo);
                                    if (listingRequired(contentPath)) {
                                        String prefix = deploymentName.substring(0, deploymentName.indexOf('.'));
                                        Path tempDir = createTempDirectory(tempBaseDir, prefix);
                                        try {
                                            for (JarInfo subJarInfo : recursiveJarAnalyzerHelper.listDeploymentContent(analyzer, tempDir, deploymentName, contentPath)) {
                                                subJarInfo.attributes().put("deployment", deploymentName);
                                                String existingArchive = processedJar.get(subJarInfo.attributes().get("path"));
                                                if (existingArchive == null || !existingArchive.equals(subJarInfo.attributes().get(SHA512_CHECKSUM_KEY))) {
                                                    processedJar.put(subJarInfo.attributes().get("path"), subJarInfo.attributes().get(SHA512_CHECKSUM_KEY));
                                                }
                                                infos.add(subJarInfo);
                                            }
                                        } finally {
                                            recursiveJarAnalyzerHelper.deleteSilentlyRecursively(tempDir);
                                        }
                                    }
                                    report.put(deploymentName, infos);
                                }
                            }
                        } else {
                            report.put(deploymentName, Collections.emptyList());
                        }
                    }
                    InsightsReportLogger.ROOT_LOGGER.endProcessingDeployments();
                }
            }
        } catch (URISyntaxException | IOException ex) {
            throw InsightsReportLogger.ROOT_LOGGER.failedToProcessDeployments(ex);
        }
    }

    public synchronized void updateReport(String deploymentName) {
        try {
            ModelNode op = Operations.createReadResourceOperation(PathAddress.pathAddress("deployment", deploymentName).toModelNode(), false);
            op.get(OPERATION_HEADERS).get("role").set("Monitor");
            op.get("include-runtime").set(true);
            op.get("include-defaults").set(true);
            ModelNode response = this.modelControllerClient.execute(op);
            if (SUCCESS.equals(response.get(OUTCOME).asString())) {
                ModelNode deployment = response.get(RESULT);
                InsightsReportLogger.ROOT_LOGGER.startProcessingDeploymentForUpdate(deploymentName);
                ModelNode content = deployment.get(CONTENT).asList().get(0);
                if (content.hasDefined(HASH)) {
                    byte[] hashBytes = content.get(HASH).asBytes();
                    String hash = HashUtil.bytesToHexString(hashBytes);
                    InsightsReportLogger.ROOT_LOGGER.deploymentHashFound(deploymentName, hash);
                    String existingDeployment = processedDeployment.get(deploymentName);
                    if (existingDeployment == null || !existingDeployment.equals(hash)) {
                        Path contentPath = contentRepository.getContent(hashBytes).getPhysicalFile().toPath();
                        if (isArchive(contentPath)) {
                            Optional<JarInfo> jarInfo = analyzer.process(deploymentName, contentPath.toUri().toURL());
                            if (jarInfo.isPresent()) {
                                JarInfo deploymentJarInfo = jarInfo.get();
                                deploymentJarInfo.attributes().put("deployment", deploymentName);
                                deploymentJarInfo.attributes().put("runtime-name",deployment.get(RUNTIME_NAME).asString());
                                deploymentJarInfo.attributes().put("path", deploymentJarInfo.name());
                                processedDeployment.put(deploymentName, hash);
                                archivesToSend.add(deploymentJarInfo);
                                if (listingRequired(contentPath)) {
                                    String prefix = deploymentName.substring(0, deploymentName.indexOf('.'));
                                    Path tempDir = createTempDirectory(tempBaseDir, prefix);
                                    try {
                                        for (JarInfo subJarInfo : recursiveJarAnalyzerHelper.listDeploymentContent(analyzer, tempDir, deploymentName, contentPath)) {
                                            subJarInfo.attributes().put("deployment", deploymentName);
                                            String existingArchive = processedJar.get(subJarInfo.attributes().get("path"));
                                            if (existingArchive == null || !existingArchive.equals(subJarInfo.attributes().get(SHA512_CHECKSUM_KEY))) {
                                                processedJar.put(subJarInfo.attributes().get("path"), subJarInfo.attributes().get(SHA512_CHECKSUM_KEY));
                                                archivesToSend.add(subJarInfo);
                                            }
                                        }
                                    } finally {
                                        recursiveJarAnalyzerHelper.deleteSilentlyRecursively(tempDir);
                                    }
                                }
                            }
                        }
                    }
                    InsightsReportLogger.ROOT_LOGGER.endProcessingDeploymentsForUpdate();
                }
            }
        } catch (URISyntaxException | IOException ex) {
            throw InsightsReportLogger.ROOT_LOGGER.failedToProcessDeployments(ex);
        }
    }

    public Map<String, List<JarInfo>> getReport() {
        return Collections.unmodifiableMap(report);
    }

    public String getProduct() {
        return "Red Hat JBoss EAP";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public JsonSerializer<InsightsSubreport> getSerializer() {
        return new JBossInsightsDeploymentSubReportSerializer();
    }
}
