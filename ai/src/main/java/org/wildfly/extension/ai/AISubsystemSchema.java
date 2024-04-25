/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.ai;

import org.wildfly.extension.ai.chat.ChatLanguageModelProviderRegistrar;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentSubsystemSchema;
import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumeration of AI subsystem schema versions.
 */
enum AISubsystemSchema implements PersistentSubsystemSchema<AISubsystemSchema> {
    VERSION_1_0(1, 0),
    ;
    static final AISubsystemSchema CURRENT = VERSION_1_0;

    private final VersionedNamespace<IntVersion, AISubsystemSchema> namespace;

    AISubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(AISubsystemRegistrar.NAME, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, AISubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public PersistentResourceXMLDescription getXMLDescription() {
        PersistentResourceXMLDescription.Factory factory = PersistentResourceXMLDescription.factory(this);
        return factory.builder(AISubsystemRegistrar.PATH)
                .addChild(factory.builder(ChatLanguageModelProviderRegistrar.PATH).addAttributes(ChatLanguageModelProviderRegistrar.ATTRIBUTES.stream()).build())
                .build();
    }
}
