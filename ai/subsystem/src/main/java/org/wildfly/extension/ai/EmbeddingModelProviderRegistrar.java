/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.ai;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;

import dev.langchain4j.model.embedding.EmbeddingModel;
import java.util.Collection;
import java.util.List;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.ai.embeddings.EmbeddingModelProviderServiceConfigurator;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

public class EmbeddingModelProviderRegistrar implements ChildResourceDefinitionRegistrar {

    static final UnaryServiceDescriptor<EmbeddingModel> EMBEDDING_MODEL_PROVIDER_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.ai.embedding.model", EmbeddingModel.class);
    public static final RuntimeCapability<Void> EMBEDDING_MODEL_PROVIDER_CAPABILITY = RuntimeCapability.Builder.of(EMBEDDING_MODEL_PROVIDER_DESCRIPTOR).setAllowMultipleRegistrations(true).build();

    public static final SimpleAttributeDefinition EMBEDDING_MODULE = new SimpleAttributeDefinitionBuilder(MODULE, ModelType.STRING, false)
            .setAllowExpression(true)
            .build();
    public static final SimpleAttributeDefinition EMBEDDING_MODEL_CLASS = new SimpleAttributeDefinitionBuilder("embedding-class", ModelType.STRING, false)
            .setAllowExpression(true)
            .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = List.of(EMBEDDING_MODULE, EMBEDDING_MODEL_CLASS);

    private final ResourceRegistration registration;
    private final ResourceDescriptor descriptor;
    static final String NAME = "embedding-model";
    static final PathElement PATH = PathElement.pathElement(NAME);

    EmbeddingModelProviderRegistrar() {
        this.registration = ResourceRegistration.of(PATH);
        this.descriptor = ResourceDescriptor.builder(AISubsystemRegistrar.RESOLVER.createChildResolver(PATH))
                .addCapability(EMBEDDING_MODEL_PROVIDER_CAPABILITY)
                .addAttributes(ATTRIBUTES)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(new EmbeddingModelProviderServiceConfigurator()))
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
