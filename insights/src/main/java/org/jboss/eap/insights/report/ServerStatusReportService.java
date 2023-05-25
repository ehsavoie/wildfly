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

import org.jboss.eap.insights.report.logging.JbossLoggingInsightsLogger;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT_DEPLOYED_NOTIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_BOOTING;
import static org.jboss.as.controller.notification.NotificationHandlerRegistry.ANY_ADDRESS;

import com.redhat.insights.InsightsReport;
import com.redhat.insights.InsightsReportController;
import com.redhat.insights.InsightsSubreport;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.core.httpclient.InsightsJdkHttpClient;
import com.redhat.insights.http.InsightsFileWritingClient;
import com.redhat.insights.http.InsightsHttpClient;
import com.redhat.insights.http.InsightsMultiClient;
import com.redhat.insights.jars.ClasspathJarInfoSubreport;
import com.redhat.insights.jars.JarInfo;
import com.redhat.insights.tls.PEMSupport;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import javax.net.ssl.SSLContext;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.notification.Notification;
import org.jboss.as.controller.notification.NotificationFilter;
import org.jboss.as.controller.notification.NotificationHandler;
import org.jboss.as.controller.notification.NotificationHandlerRegistry;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironment.LaunchType;
import org.jboss.dmr.ModelNode;
import org.jboss.eap.insights.report.logging.InsightsReportLogger;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 *
 * @author Emmanuel Hugonnet (c) 2023 Red Hat, Inc.
 */
class ServerStatusReportService implements Service, PropertyChangeListener, NotificationHandler {

    private final Supplier<ProcessStateNotifier> notifierSupplier;
    private final Supplier<ModelControllerClientFactory> clientFactorySupplier;
    private final Supplier<ScheduledExecutorService> scheduledExecutorSupplier;
    private final Supplier<ExecutorService> executorServiceSupplier;
    private final Supplier<NotificationHandlerRegistry> notificationRegistrySupplier;
    private final Supplier<ServerEnvironment> serverEnvironmentSupplier;
    private final Supplier<ContentRepository> contentRepositorySupplier;
    private Supplier<SSLContext> sslContextSupplier;
    private final Supplier<InsightsConfiguration> configurationSupplier;
    private boolean reporting = false;

    private InsightsReportController insightsReportController;
    private JBossInsightsDeploymentSubReport wildFlyInsightsDeploymentSubReport;
    private static final NotificationFilter DEPLOYMENT_FILTER = (Notification notification) -> {
        if (DEPLOYMENT_DEPLOYED_NOTIFICATION.equals(notification.getType())) {
            ModelNode notificationData = notification.getData();
            if (notificationData.hasDefined(SERVER_BOOTING) && notificationData.get(SERVER_BOOTING).asBoolean()) {
                return false;
            }
            return true;
        }
        return false;
    };

    ServerStatusReportService(Supplier<ProcessStateNotifier> notifierSupplier,
            Supplier<ModelControllerClientFactory> clientFactorySupplier,
            Supplier<ScheduledExecutorService> scheduledExecutorSupplier,
            Supplier<ProcessStateNotifier> processStateNotifierSupplier,
            Supplier<ExecutorService> executorServiceSupplier,
            Supplier<NotificationHandlerRegistry> notificationRegistrySupplier,
            Supplier<ContentRepository> contentRepositorySupplier,
            Supplier<ServerEnvironment> serverEnvironmentSupplier,
            Supplier<SSLContext> sslContextSupplier,
            Supplier<InsightsConfiguration> configurationSupplier) {
        this.notifierSupplier = notifierSupplier;
        this.clientFactorySupplier = clientFactorySupplier;
        this.scheduledExecutorSupplier = scheduledExecutorSupplier;
        this.executorServiceSupplier = executorServiceSupplier;
        this.notificationRegistrySupplier = notificationRegistrySupplier;
        this.contentRepositorySupplier = contentRepositorySupplier;
        this.serverEnvironmentSupplier = serverEnvironmentSupplier;
        this.configurationSupplier = configurationSupplier;
        final PEMSupport pemSupport = new PEMSupport(JbossLoggingInsightsLogger.INSTANCE, configurationSupplier.get());
        this.sslContextSupplier = sslContextSupplier != null ? sslContextSupplier : new Supplier<SSLContext>() {
            @Override
            public SSLContext get() {
                try {
                    return pemSupport.createTLSContext();
                } catch (Exception ex) {
                    InsightsReportLogger.ROOT_LOGGER.debug("Error getting certificate", ex);
                    throw ex;
                }
            }

        };
    }

    @Override
    public void start(StartContext context) throws StartException {
        final ServerEnvironment serverEnvironment = serverEnvironmentSupplier.get();
        final InsightsConfiguration configuration = configurationSupplier.get();
        if (!shoudStartInsights(serverEnvironment, configuration)) {
            return;
        }
        reporting = true;
        notifierSupplier.get().addPropertyChangeListener(this);
        notificationRegistrySupplier.get().registerNotificationHandler(ANY_ADDRESS, this, DEPLOYMENT_FILTER);
        boolean useYaml = serverEnvironment.getConfigurationExtension() != null;
        JBossInsightsSubReport wildFlyInsightsSubReport = new JBossInsightsSubReport(clientFactorySupplier.get(), executorServiceSupplier.get());
        JBossJarInfoModuleSubReport wildFlyJarInfoModuleSubReport = new JBossJarInfoModuleSubReport();
        BlockingQueue<JarInfo> archivesToSend = new LinkedBlockingQueue<>();
        this.wildFlyInsightsDeploymentSubReport = new JBossInsightsDeploymentSubReport(
                clientFactorySupplier.get(), executorServiceSupplier.get(), this.contentRepositorySupplier.get(), this.serverEnvironmentSupplier.get().getServerTempDir().toPath(), archivesToSend);
        JBossInsightsWrapperSubReport subReport = new JBossInsightsWrapperSubReport(serverEnvironment.getProductConfig(), wildFlyInsightsSubReport, wildFlyJarInfoModuleSubReport, wildFlyInsightsDeploymentSubReport, useYaml, serverEnvironment.useGit());
        Map<String, InsightsSubreport> subReports = new HashMap<>(2);
        subReports.put("jars", new ClasspathJarInfoSubreport(JbossLoggingInsightsLogger.INSTANCE));
        subReports.put("eap", subReport);
        InsightsReport report = new JBossTopLevelReport(JbossLoggingInsightsLogger.INSTANCE, configuration, subReports, "Red Hat JBoss EAP " + serverEnvironment.getInstanceUuid());
        Supplier<InsightsHttpClient> httpClientSupplier = new Supplier<InsightsHttpClient>() {
            @Override
            public InsightsHttpClient get() {
                return new InsightsMultiClient(
                        JbossLoggingInsightsLogger.INSTANCE,
                        new InsightsJdkHttpClient(JbossLoggingInsightsLogger.INSTANCE, configuration, sslContextSupplier),
                        new InsightsFileWritingClient(JbossLoggingInsightsLogger.INSTANCE, configuration));
            }
        };
        insightsReportController = InsightsReportController.of(JbossLoggingInsightsLogger.INSTANCE, configuration, report, httpClientSupplier,
                new JBossInsightsScheduler(scheduledExecutorSupplier.get(), configuration),
                archivesToSend);
    }

    private boolean shoudStartInsights(ServerEnvironment serverEnvironment, InsightsConfiguration configuration) {
        return !configuration.isOptingOut()
                && RunningMode.NORMAL == serverEnvironment.getInitialRunningMode()
                && (LaunchType.STANDALONE == serverEnvironment.getLaunchType()
                || LaunchType.DOMAIN == serverEnvironment.getLaunchType());
    }

    @Override
    public void stop(StopContext context) {
        if (reporting) {
            notifierSupplier.get().removePropertyChangeListener(this);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("currentState".equals(evt.getPropertyName())) {
            ControlledProcessState.State state = (ControlledProcessState.State) evt.getNewValue();
            switch (state) {
                case RUNNING:
                    try {
                    insightsReportController.generate();
                } catch (Exception ex) {
                    InsightsReportLogger.ROOT_LOGGER.debug("Unexpected error", ex);
                }
                break;
                case STOPPED:
                    break;
                default:
            }
        }
    }

    @Override
    public void handleNotification(Notification notification) {
        InsightsReportLogger.ROOT_LOGGER.debug("Some deployment change " + notification);
        wildFlyInsightsDeploymentSubReport.updateReport(notification.getSource().getLastElement().getValue());
    }
}
