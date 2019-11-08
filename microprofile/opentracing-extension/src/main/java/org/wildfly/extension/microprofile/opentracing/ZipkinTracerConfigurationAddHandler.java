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

import static org.wildfly.extension.microprofile.opentracing.SubsystemDefinition.TRACER_CAPABILITY;
import static org.wildfly.extension.microprofile.opentracing.ZipkinTracerConfigurationDefinition.ATTRIBUTES;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.microprofile.opentracing.resolver.ZipkinTracerConfiguration;
import org.wildfly.microprofile.opentracing.smallrye.TracerConfiguration;

/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class ZipkinTracerConfigurationAddHandler extends AbstractAddStepHandler {

    static final ZipkinTracerConfigurationAddHandler INSTANCE = new ZipkinTracerConfigurationAddHandler();

    private ZipkinTracerConfigurationAddHandler() {
        super( ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        super.performRuntime(context, operation, model);
        ServiceName serviceName = TRACER_CAPABILITY.getCapabilityServiceName(context.getCurrentAddressValue());
        ServiceBuilder builder = context.getServiceTarget().addService(serviceName);
        TracerConfiguration config = new ZipkinTracerConfiguration(context, operation);
        OpentracingConfigurationService.installTracerConfigurationService(builder, config, serviceName);
    }

}
