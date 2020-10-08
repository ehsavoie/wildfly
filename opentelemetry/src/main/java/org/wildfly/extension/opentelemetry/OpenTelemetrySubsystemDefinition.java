package org.wildfly.extension.opentelemetry;

/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
import static org.wildfly.extension.opentelemetry.OpenTelemetryExtension.SUBSYSTEM_PATH;

import java.util.Collection;
import java.util.Collections;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;

public class OpenTelemetrySubsystemDefinition extends PersistentResourceDefinition {

    private static final String OPENTELEMETRY_CAPABILITY_NAME = "org.wildfly.extension.opentelemetry";

    private static final RuntimeCapability<Void> OPENTELEMETRY_CAPABILITY = RuntimeCapability.Builder
            .of(OPENTELEMETRY_CAPABILITY_NAME)
            .build();

    static final String[] MODULES = {
        "io.opentelemetry.opentelemetry-api","io.opentelemetry.opentelemetry-sdk",
        "io.smallrye.opentelemetry.api","io.smallrye.opentelemetry.jaxrs2"};

    static final String[] EXPORTED_MODULES = {
        };

    protected OpenTelemetrySubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(SUBSYSTEM_PATH, OpenTelemetryExtension.getResourceDescriptionResolver())
                .setAddHandler(SubsystemAdd.INSTANCE)
                .setCapabilities(OPENTELEMETRY_CAPABILITY)
        );
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        super.registerChildren(resourceRegistration);
    }

    @Override
    public void registerAdditionalRuntimePackages(final ManagementResourceRegistration resourceRegistration) {
        for (String m : MODULES) {
            resourceRegistration.registerAdditionalRuntimePackages(RuntimePackageDependency.required(m));
        }
        for (String m : EXPORTED_MODULES) {
            resourceRegistration.registerAdditionalRuntimePackages(RuntimePackageDependency.required(m));
        }
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }
}
