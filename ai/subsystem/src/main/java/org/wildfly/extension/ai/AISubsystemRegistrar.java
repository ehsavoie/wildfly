/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.deployment.Phase;
import org.wildfly.extension.ai.chat.ChatLanguageModelProviderRegistrar;
import org.wildfly.extension.ai.deployment.ChatLanguageModelDependencyProcessor;
import org.wildfly.extension.ai.deployment.ChatLanguageModelDeploymentProcessor;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;
import org.wildfly.subsystem.service.capture.ServiceValueExecutorRegistry;
import org.wildfly.subsystem.service.capture.ServiceValueRegistry;

/**
 * Registrar for the AI subsystem.
 */
class AISubsystemRegistrar implements SubsystemResourceDefinitionRegistrar {

    static final String NAME = "ai";
    static final PathElement PATH = SubsystemResourceDefinitionRegistrar.pathElement(NAME);
    static final ParentResourceDescriptionResolver RESOLVER = new SubsystemResourceDescriptionResolver(NAME, AISubsystemRegistrar.class);

    @Override
    public ManagementResourceRegistration register(SubsystemRegistration parent, ManagementResourceRegistrationContext context) {
        ServiceValueRegistry<ChatLanguageModel> chatModelRegistry = ServiceValueExecutorRegistry.newInstance();
        parent.setHostCapable();
        ManagementResourceRegistration registration = parent.registerSubsystemModel(ResourceDefinition.builder(ResourceRegistration.of(PATH), RESOLVER).build());
        ResourceDescriptor descriptor = ResourceDescriptor
                .builder(RESOLVER)
                .withDeploymentChainContributor(target -> {
                    target.addDeploymentProcessor(NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_MICROPROFILE_OPENTRACING, new ChatLanguageModelDependencyProcessor());
                    target.addDeploymentProcessor(NAME, Phase.POST_MODULE, Phase.POST_MODULE_MICROPROFILE_OPENTRACING, new ChatLanguageModelDeploymentProcessor());
                })
                .build();
        ManagementResourceRegistrar.of(descriptor).register(registration);
        new ChatLanguageModelProviderRegistrar(RESOLVER, chatModelRegistry).register(registration, context);
        new EmbeddingModelProviderRegistrar().register(registration, context);
        new EmbeddingStoreProviderRegistrar().register(registration, context);
        return registration;
    }
}
