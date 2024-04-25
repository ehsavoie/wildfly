/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.ai.injection;

import dev.langchain4j.model.chat.ChatLanguageModel;
import io.smallrye.common.annotation.Identifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Emmanuel Hugonnet (c) 2024 Red Hat, Inc.
 */
public class AiCDIExtension implements Extension {

    private static final Map<String, ChatLanguageModel> chatModels = new HashMap<>();


    public static final void registerChatLanguageModel(String id, ChatLanguageModel chatModel) {
        chatModels.put(id, chatModel);
    }

    public void registerChatLanguageModelBean(@Observes AfterBeanDiscovery abd, BeanManager beanManager) {
        for (Map.Entry<String, ChatLanguageModel> entry : chatModels.entrySet()) {
            abd.addBean()
                    .scope(ApplicationScoped.class)
                    .addQualifier(Identifier.Literal.of(entry.getKey()))
                    .types(ChatLanguageModel.class)
                    .createWith(c -> entry.getValue());
        }
    }
}
