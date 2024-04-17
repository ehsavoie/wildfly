/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.ai.embeddings;

import dev.langchain4j.model.embedding.EmbeddingModel;
import java.util.function.Supplier;


public interface EmbeddingModelProvider extends Supplier<EmbeddingModel>{
}

