/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.ServiceValueRegistry;

/**
 *
 * @author Emmanuel Hugonnet (c) 2024 Red Hat, Inc.
 */
public class ChatLanguageModelRegistry implements ServiceValueRegistry<ChatLanguageModel> {

    private final Map<ServiceDependency<ChatLanguageModel>, AtomicReference<ChatLanguageModel>> references = new ConcurrentHashMap<>();

    private AtomicReference<ChatLanguageModel> create(ServiceDependency<ChatLanguageModel> dependency) {
        return new AtomicReference<>();
    }

    @Override
    public Consumer<ChatLanguageModel> add(ServiceDependency<ChatLanguageModel> key) {
        AtomicReference<ChatLanguageModel> reference = this.references.computeIfAbsent(key, this::create);
        return reference::set;
    }

    @Override
    public void remove(ServiceDependency<ChatLanguageModel> key) {
        AtomicReference<ChatLanguageModel> reference = this.references.remove(key);
        if (reference != null) {
            reference.set(null);
        }
    }

}
