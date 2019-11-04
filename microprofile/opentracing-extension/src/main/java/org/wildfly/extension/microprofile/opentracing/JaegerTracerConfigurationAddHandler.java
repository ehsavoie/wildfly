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

import static org.wildfly.extension.microprofile.opentracing.JaegerTracerConfigurationDefinition.ATTRIBUTES;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.microprofile.opentracing.resolver.JaegerTracerConfiguration;
import org.wildfly.microprofile.opentracing.smallrye.TracerConfiguration;

/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class JaegerTracerConfigurationAddHandler extends AbstractAddStepHandler {

    static final JaegerTracerConfigurationAddHandler INSTANCE = new JaegerTracerConfigurationAddHandler();

    private JaegerTracerConfigurationAddHandler() {
        super(ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        super.performRuntime(context, operation, model);
        TracerConfiguration config = new JaegerTracerConfiguration(context, operation);
        OpentracingConfigurationService.installTracerConfigurationService(context.getServiceTarget(), config, context.getCurrentAddressValue());
    }

}
