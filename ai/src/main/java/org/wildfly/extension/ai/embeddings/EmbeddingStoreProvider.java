/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.ai.embeddings;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.function.Supplier;

public interface EmbeddingStoreProvider extends Supplier<EmbeddingStore<TextSegment>>{
}

