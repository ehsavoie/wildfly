/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.ai.deployment;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.ai.AILogger.ROOT_LOGGER;
import static org.wildfly.extension.ai.Capabilities.CHAT_MODEL_PROVIDER_CAPABILITY;
import static org.wildfly.extension.ai.Capabilities.EMBEDDING_MODEL_PROVIDER_CAPABILITY;
import static org.wildfly.extension.ai.Capabilities.EMBEDDING_STORE_PROVIDER_CAPABILITY;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.smallrye.common.annotation.Identifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.weld.WeldCapability;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.Type;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.extension.ai.injection.AiCDIExtension;

/**
 *
 * @author Emmanuel Hugonnet (c) 2024 Red Hat, Inc.
 */
public class AIDeploymentProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        try {
            final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
            final WeldCapability weldCapability = support.getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
            if (weldCapability != null && !weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
                ROOT_LOGGER.cdiRequired();
            }
            final CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
            if (index == null) {
                throw ROOT_LOGGER.unableToResolveAnnotationIndex(deploymentUnit);
            }
            List<AnnotationInstance> annotations = index.getAnnotations(DotName.createSimple(Identifier.class));
            if (annotations == null || annotations.isEmpty()) {
                return;
            }
            Set<String> requiredChatModels = new HashSet<>();
            Set<String> requiredEmbeddingModels = new HashSet<>();
            Set<String> requiredEmbeddingStores = new HashSet<>();
            for (AnnotationInstance annotation : annotations) {
                if (annotation.target().kind() == AnnotationTarget.Kind.FIELD) {
                    FieldInfo field = annotation.target().asField();
                    if (field.type().kind() == Type.Kind.CLASS) {
                        if (field.toString().startsWith("dev.langchain4j.model.chat.ChatLanguageModel")) {
                            ROOT_LOGGER.warn("We need the ChatLanguageModel in the class " + field.declaringClass());
                            String chatLanguageModelName = annotation.value().asString();
                            ROOT_LOGGER.warn("We need the ChatLanguageModel called " + chatLanguageModelName);
                            requiredChatModels.add(chatLanguageModelName);
                        } else if (field.toString().startsWith("dev.langchain4j.model.embedding.EmbeddingModel")) {
                            ROOT_LOGGER.warn("We need the EmbeddingModel in the class " + field.declaringClass());
                            String embeddingModelName = annotation.value().asString();
                            ROOT_LOGGER.warn("We need the EmbeddingModel called " + embeddingModelName);
                            requiredEmbeddingModels.add(embeddingModelName);
                        } else if (field.toString().startsWith("dev.langchain4j.store.embedding.EmbeddingStore")) {
                            ROOT_LOGGER.warn("We need the EmbeddingStore in the class " + field.declaringClass());
                            String embeddingStoreName = annotation.value().asString();
                            ROOT_LOGGER.warn("We need the EmbeddingStore called " + embeddingStoreName);
                            requiredEmbeddingStores.add(embeddingStoreName);
                        }
                    }
                }
            }
            if (!requiredChatModels.isEmpty() || !requiredEmbeddingModels.isEmpty() || ! requiredEmbeddingStores.isEmpty()) {
                ServiceBuilder builder = deploymentPhaseContext.getRequirementServiceTarget().addService();
                if (!requiredChatModels.isEmpty()) {
                    for (String chatLanguageModelName : requiredChatModels) {
                        AiCDIExtension.registerChatLanguageModel(chatLanguageModelName,
                                (ChatLanguageModel) builder.requires(CHAT_MODEL_PROVIDER_CAPABILITY.getCapabilityServiceName(chatLanguageModelName)).get());
                    }
                }
                if (!requiredEmbeddingModels.isEmpty()) {
                    for (String embeddingModelName : requiredEmbeddingModels) {
                        AiCDIExtension.registerEmbeddingModel(embeddingModelName,
                                (EmbeddingModel) builder.requires(EMBEDDING_MODEL_PROVIDER_CAPABILITY.getCapabilityServiceName(embeddingModelName)).get());
                    }
                }
                if (!requiredEmbeddingStores.isEmpty()) {
                    for (String embeddingStoreName : requiredEmbeddingStores) {
                        AiCDIExtension.registerEmbeddingStore(embeddingStoreName,
                                (EmbeddingStore) builder.requires(EMBEDDING_STORE_PROVIDER_CAPABILITY.getCapabilityServiceName(embeddingStoreName)).get());
                    }
                }
                builder.install();
                support.getOptionalCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class).get()
                        .registerExtensionInstance(new AiCDIExtension(), deploymentUnit);
            }
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
