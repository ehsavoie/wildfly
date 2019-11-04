/*
 * Copyright 2019 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.microprofile.opentracing;

import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.COMPRESSION;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.CONNECTION_TIMEOUT;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.ENCODING;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.ENDPOINT;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.MAX_REQUEST;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.MESSAGE_MAX_BYTES;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.READ_TIMEOUT;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SAMPLER;
import static org.wildfly.microprofile.opentracing.smallrye.WildFlyTracerFactory.TRACER_CAPABILITY_NAME;

import java.util.Arrays;
import java.util.Collection;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.RuntimePackageDependency;

/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class ZipkinTracerConfigurationDefinition extends PersistentResourceDefinition {

    public static final RuntimeCapability<Void> TRACER_CAPABILITY = RuntimeCapability.Builder
            .of(TRACER_CAPABILITY_NAME, true, OpentracingConfigurationService.class)
            .build();

    public static final PathElement TRACER_CONFIGURATION_PATH = PathElement.pathElement("zipkin-tracer");

    public static final AttributeDefinition[] ATTRIBUTES = {ENDPOINT, ENCODING, CONNECTION_TIMEOUT, READ_TIMEOUT, MAX_REQUEST, MESSAGE_MAX_BYTES, COMPRESSION, SAMPLER};

    public ZipkinTracerConfigurationDefinition() {
        super(new SimpleResourceDefinition.Parameters(TRACER_CONFIGURATION_PATH, SubsystemExtension.getResourceDescriptionResolver("tracer"))
                .setAddHandler(ZipkinTracerConfigurationAddHandler.INSTANCE)
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(TRACER_CAPABILITY)
                .setAdditionalPackages(RuntimePackageDependency.required("io.zipkin.brave")));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

}
