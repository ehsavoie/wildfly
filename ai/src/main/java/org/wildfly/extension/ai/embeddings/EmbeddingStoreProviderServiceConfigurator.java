/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.ai.embeddings;

import static org.wildfly.extension.ai.EmbeddingModelProviderRegistrar.EMBEDDING_MODULE;
import static org.wildfly.extension.ai.EmbeddingStoreProviderRegistrar.STORE_TYPE;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import java.util.function.Supplier;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.ai.EmbeddingStoreProviderRegistrar;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

public class EmbeddingStoreProviderServiceConfigurator implements ResourceServiceConfigurator {

    public EmbeddingStoreProviderServiceConfigurator() {
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String moduleName = EMBEDDING_MODULE.resolveModelAttribute(context, model).asString();
        String storeType = STORE_TYPE.resolveModelAttribute(context, model).asStringOrNull();
        Supplier<EmbeddingStore<TextSegment>> factory = new Supplier<>() {
            @Override
            public EmbeddingStore<TextSegment> get() {
                ClassLoader tccl = Thread.currentThread().getContextClassLoader();
                try {
                    ClassLoader moduleCL = org.jboss.modules.Module.getCallerModuleLoader().loadModule(moduleName).getClassLoader();
                     Thread.currentThread().setContextClassLoader(moduleCL);
                    return new InMemoryEmbeddingStore();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                    Thread.currentThread().setContextClassLoader(tccl);
                }
            }
        };
        return CapabilityServiceInstaller.builder(EmbeddingStoreProviderRegistrar.EMBEDDING_STORE_PROVIDER_CAPABILITY, factory).asActive().build();
    }

}
