/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.ai.chat;

import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.function.Supplier;

/**
 *
 * @author Emmanuel Hugonnet (c) 2024 Red Hat, Inc.
 */
public interface ChatLanguageModelProvider extends Supplier<ChatLanguageModel>{
}
