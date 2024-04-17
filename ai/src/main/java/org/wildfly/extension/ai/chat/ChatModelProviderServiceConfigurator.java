/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.ai;

import static org.wildfly.extension.ai.ChatLanguageModelProviderRegistrar.BASE_URL;
import static org.wildfly.extension.ai.ChatLanguageModelProviderRegistrar.CHAT_MODEL_PROVIDER_CAPABILITY;
import static org.wildfly.extension.ai.ChatLanguageModelProviderRegistrar.CONNECT_TIMEOUT;
import static org.wildfly.extension.ai.ChatLanguageModelProviderRegistrar.TEMPERATURE;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.time.Duration;
import java.util.function.Supplier;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Configures an aggregate ChatModel provider service.
 */
public class ChatModelProviderServiceConfigurator implements ResourceServiceConfigurator {

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        double temperature = TEMPERATURE.resolveModelAttribute(context, model).asDouble();
        long connectTimeOut = CONNECT_TIMEOUT.resolveModelAttribute(context, model).asLong();
        String baseUrl = BASE_URL.resolveModelAttribute(context, model).asString("http://langchain4j.dev/demo/openai/v1");
        Supplier<ChatLanguageModel> factory = new Supplier<>() {
            @Override
            public ChatLanguageModel get() {
                ChatLanguageModel model =  OpenAiChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey("demo")
                        .maxRetries(5)
                        .temperature(temperature)
                        .timeout(Duration.ofMillis(connectTimeOut))
                        .logRequests(Boolean.TRUE)
                        .logResponses(Boolean.TRUE)
                        .maxTokens(1000)
                        .build();
                model.generate(new UserMessage("Generate me a haiku please"));
                return model;
            }
        };
        return CapabilityServiceInstaller.builder(CHAT_MODEL_PROVIDER_CAPABILITY, factory).asActive().build();
    }
}
