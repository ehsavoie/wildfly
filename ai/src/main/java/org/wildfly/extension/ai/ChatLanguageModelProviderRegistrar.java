/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.Collection;
import java.util.List;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

public class ChatLanguageModelProviderRegistrar implements ChildResourceDefinitionRegistrar {

    static final UnaryServiceDescriptor<ChatLanguageModel> CHAT_MODEL_PROVIDER_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.ai.chatmodel", ChatLanguageModel.class);
    static final RuntimeCapability<Void> CHAT_MODEL_PROVIDER_CAPABILITY = RuntimeCapability.Builder.of(CHAT_MODEL_PROVIDER_DESCRIPTOR).setAllowMultipleRegistrations(true).build();

    public static final SimpleAttributeDefinition BASE_URL = new SimpleAttributeDefinitionBuilder("base-url", ModelType.STRING, false).setAllowExpression(true).build();
    public static final SimpleAttributeDefinition CONNECT_TIMEOUT = new SimpleAttributeDefinitionBuilder("connect-timeout", ModelType.LONG, true)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.ZERO)
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .build();
    public static final SimpleAttributeDefinition TEMPERATURE = new SimpleAttributeDefinitionBuilder("temperature", ModelType.DOUBLE, true)
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.ZERO)
            .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = List.of(BASE_URL,CONNECT_TIMEOUT,TEMPERATURE);

    private final ResourceRegistration registration;
    private final ResourceDescriptor descriptor;
    static final String NAME = "chat-model";
    static final PathElement PATH = PathElement.pathElement(NAME);

    ChatLanguageModelProviderRegistrar() {
        this.registration = ResourceRegistration.of(PATH);
        this.descriptor =  ResourceDescriptor.builder(AISubsystemRegistrar.RESOLVER.createChildResolver(PATH))
                .addCapability(CHAT_MODEL_PROVIDER_CAPABILITY)
                .addAttributes(ATTRIBUTES)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(new ChatModelProviderServiceConfigurator()))
                .build();
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDefinition definition = ResourceDefinition.builder(this.registration, this.descriptor.getResourceDescriptionResolver()).build();
        ManagementResourceRegistration subsystemRegistration = parent.registerSubModel(definition);
        ManagementResourceRegistrar.of(this.descriptor).register(subsystemRegistration);
        return subsystemRegistration;
    }
}
