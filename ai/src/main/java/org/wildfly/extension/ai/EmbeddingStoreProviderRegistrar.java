/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.ai;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODULE;

import org.wildfly.extension.ai.embeddings.EmbeddingStoreProviderServiceConfigurator;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

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
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;

public class EmbeddingStoreProviderRegistrar implements ChildResourceDefinitionRegistrar {

    static final UnaryServiceDescriptor<EmbeddingModel> EMBEDDING_STORE_PROVIDER_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.ai.embedding.store", EmbeddingModel.class);
    public static final RuntimeCapability<Void> EMBEDDING_STORE_PROVIDER_CAPABILITY = RuntimeCapability.Builder.of(EMBEDDING_STORE_PROVIDER_DESCRIPTOR).setAllowMultipleRegistrations(true).build();

    public static final SimpleAttributeDefinition STORE_TYPE = new SimpleAttributeDefinitionBuilder(TYPE, ModelType.STRING, false)
            .setAllowedValues("in-memory")
            .setAllowExpression(true)
            .build();
    public static final SimpleAttributeDefinition EMBEDDING_MODULE = new SimpleAttributeDefinitionBuilder(MODULE, ModelType.STRING, true)
            .setDefaultValue(new ModelNode("dev.langchain4j"))
            .setAllowExpression(true)
            .build();
    static final Collection<AttributeDefinition> ATTRIBUTES = List.of(STORE_TYPE, EMBEDDING_MODULE);

    private final ResourceRegistration registration;
    private final ResourceDescriptor descriptor;
    static final String NAME = "embedding-store";
    static final PathElement PATH = PathElement.pathElement(NAME);

    EmbeddingStoreProviderRegistrar() {
        this.registration = ResourceRegistration.of(PATH);
        this.descriptor = ResourceDescriptor.builder(AISubsystemRegistrar.RESOLVER.createChildResolver(PATH))
                .addCapability(EMBEDDING_STORE_PROVIDER_CAPABILITY)
                .addAttributes(ATTRIBUTES)
                .withRuntimeHandler(ResourceOperationRuntimeHandler.configureService(new EmbeddingStoreProviderServiceConfigurator()))
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
