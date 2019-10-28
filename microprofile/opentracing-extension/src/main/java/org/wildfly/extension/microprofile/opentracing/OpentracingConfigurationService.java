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



import static org.wildfly.extension.microprofile.opentracing.TracerConfigurationDefinition.TRACER_CAPABILITY;

import java.util.function.Consumer;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import org.wildfly.microprofile.opentracing.smallrye.TracerConfiguration;
import org.wildfly.microprofile.opentracing.smallrye.WildFlyTracerFactory;

/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class OpentracingConfigurationService implements Service {

    private final TracerConfiguration config;
    private final Consumer<TracerConfiguration> consumer;

    public OpentracingConfigurationService(Consumer<TracerConfiguration> consumer, TracerConfiguration config) {
        this.config = config;
        this.consumer = consumer;
    }

    @Override
    public void start(StartContext context) throws StartException {
        consumer.accept(config);
    }

    @Override
    public void stop(StopContext context) {
        consumer.accept(null);
    }


    @SuppressWarnings("unchecked")
    public static final void installJaeger(ServiceTarget target, TracerConfiguration config, String tracerName) {
        ServiceName serviceName = TRACER_CAPABILITY.getCapabilityServiceName(tracerName);
        ServiceBuilder builder = target.addService(serviceName);
        builder.setInstance(new OpentracingConfigurationService(WildFlyTracerFactory.registerTracer(serviceName), config));
        builder.install();
    }
}
