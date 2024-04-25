/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.ai.deployment;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.ai.AILogger.ROOT_LOGGER;
import static org.wildfly.extension.ai.chat.ChatLanguageModelProviderRegistrar.CHAT_MODEL_PROVIDER_CAPABILITY;

import dev.langchain4j.model.chat.ChatLanguageModel;
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
import org.wildfly.extension.ai.AiCDIExtension;

/**
 *
 * @author Emmanuel Hugonnet (c) 2024 Red Hat, Inc.
 */
public class ChatLanguageModelDeploymentProcessor implements DeploymentUnitProcessor {

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
            for (AnnotationInstance annotation : annotations) {
                if (annotation.target().kind() == AnnotationTarget.Kind.FIELD) {
                    FieldInfo field = annotation.target().asField();
                    if (field.type().kind() == Type.Kind.CLASS) {
                        String chatLanguageModelName = annotation.value().asString();
                        ROOT_LOGGER.warn("We need the ChatLanguageModel called " + chatLanguageModelName);
                        requiredChatModels.add(chatLanguageModelName);
                    }
                }
            }
            if (!requiredChatModels.isEmpty()) {
                ServiceBuilder builder = deploymentPhaseContext.getRequirementServiceTarget().addService();
                for (String chatLanguageModelName : requiredChatModels) {
                    AiCDIExtension.registerChatLanguageModel(chatLanguageModelName,
                            (ChatLanguageModel) builder.requires(CHAT_MODEL_PROVIDER_CAPABILITY.getCapabilityServiceName(chatLanguageModelName)).get());
                }
                builder.install();
            }
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }
}
