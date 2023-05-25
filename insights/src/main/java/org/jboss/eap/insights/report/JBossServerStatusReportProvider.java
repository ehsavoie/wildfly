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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.controller.ManagementModel;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.ModelControllerServiceInitialization;
import org.jboss.as.controller.ProcessStateNotifier;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.notification.NotificationHandlerRegistry;
import org.jboss.as.repository.ContentRepository;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.server.ServerService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 *
 * @author Emmanuel Hugonnet (c) 2023 Red Hat, Inc.
 */
public final class JBossServerStatusReportProvider implements ModelControllerServiceInitialization {

    private static final String EXECUTOR_CAPABILITY_NAME = "org.wildfly.management.executor";
    private static final String CLIENT_FACTORY_CAPABILITY_NAME = "org.wildfly.management.model-controller-client-factory";
    private static final String PROCESS_STATE_NOTIFIER_CAPABILITY_NAME = "org.wildfly.management.process-state-notifier";
    private static final String NOTIFICATION_REGISTRY_CAPABILITY_NAME = "org.wildfly.management.notification-handler-registry";

    private static final RuntimeCapability<Void> SERVER_REPORT_CAPABILITY =
            RuntimeCapability.Builder.of("org.wildfly.management.server.reporter", ServerStatusReportService.class)
                    .addRequirements(EXECUTOR_CAPABILITY_NAME, PROCESS_STATE_NOTIFIER_CAPABILITY_NAME,
                            CLIENT_FACTORY_CAPABILITY_NAME, NOTIFICATION_REGISTRY_CAPABILITY_NAME)
                    .build();
    /** Capability in-vm users of the controller use to register notification handlers */
    private static final RuntimeCapability<Void> NOTIFICATION_REGISTRY_CAPABILITY =
            RuntimeCapability.Builder.of(NOTIFICATION_REGISTRY_CAPABILITY_NAME, NotificationHandlerRegistry.class)
                    .build();

    @SuppressWarnings("unchecked")
    public void initializeCoreServices(ServiceTarget target) {
        ServiceBuilder builder = target.addService(SERVER_REPORT_CAPABILITY.getCapabilityServiceName());
        final Supplier<ModelControllerClientFactory> mccfSupplier = builder.requires(ServerService.JBOSS_SERVER_CLIENT_FACTORY);
        final Supplier<ScheduledExecutorService> sesSupplier = builder.requires(ServerService.JBOSS_SERVER_SCHEDULED_EXECUTOR);
        final Supplier<ExecutorService> esSupplier = builder.requires(ServerService.EXECUTOR_CAPABILITY.getCapabilityServiceName());
        final Supplier<ProcessStateNotifier> processStateSuppplier = builder.requires(ControlledProcessStateService.INTERNAL_SERVICE_NAME);
        final Supplier<NotificationHandlerRegistry> notificationRegistrySupplier = builder.requires(NOTIFICATION_REGISTRY_CAPABILITY.getCapabilityServiceName());
        final Supplier<ContentRepository> contentRepositorySupplier = builder.requires(ContentRepository.SERVICE_NAME);
        final Supplier<ServerEnvironment> serverEnvironmentSupplier = builder.requires(ServerEnvironmentService.SERVICE_NAME);
        ServerStatusReportService service = new ServerStatusReportService(processStateSuppplier, mccfSupplier,
                sesSupplier, processStateSuppplier, esSupplier, notificationRegistrySupplier,
                contentRepositorySupplier, serverEnvironmentSupplier, null, JBossInsightsConfiguration::new);
        builder.setInstance(service);
        builder.setInitialMode(ServiceController.Mode.ACTIVE);
        builder.install();
    }

    @Override
    public void initializeStandalone(ServiceTarget target, ManagementModel managementModel) {
        initializeCoreServices(target);
    }

    @Override
    public void initializeDomain(ServiceTarget target, ManagementModel managementModel) {
        //NO-OP
    }

    @Override
    public void initializeHost(ServiceTarget target, ManagementModel managementModel, String hostName) {
        //NO-OP
    }

}
