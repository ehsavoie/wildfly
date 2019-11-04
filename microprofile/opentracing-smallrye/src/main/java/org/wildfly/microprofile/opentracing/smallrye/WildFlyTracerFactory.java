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
package org.wildfly.microprofile.opentracing.smallrye;

import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.msc.service.ServiceName;

/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class WildFlyTracerFactory {

    private static final Map<ServiceName, Configuration> CONFIGURATIONS = new HashMap<>();

    public static final String TRACER_CAPABILITY_NAME = "org.wildfly.microprofile.opentracing.tracer";
    private static final RuntimeCapability<Void> TRACER_CAPABILITY = RuntimeCapability.Builder
            .of(TRACER_CAPABILITY_NAME, true, Void.class)
            .build();

    public static Consumer<TracerConfiguration> registerTracer(ServiceName service) {
        return CONFIGURATIONS.computeIfAbsent(service, serviceName -> new Configuration());
    }

    @SuppressWarnings("unchecked")
    public static Tracer getTracer(String config, String serviceName) {
        if (config != null && serviceName != null) {
            final ServiceName service = TRACER_CAPABILITY.getCapabilityServiceName(config);
            TracerConfiguration configuration = CONFIGURATIONS.get(service).get();
            if (configuration != null) {
                return configuration.createTracer(serviceName);
            }
        }
        return NoopTracerFactory.create();
    }

    @SuppressWarnings("unchecked")
    public static List<String> getModules() {
       return CONFIGURATIONS.values().stream().map(Configuration::get).filter(config -> config != null).map(TracerConfiguration::getModuleName).collect(Collectors.toList());
    }

    private static final class Configuration implements Consumer<TracerConfiguration> {
        private TracerConfiguration t;
        @Override
        public void accept(TracerConfiguration t) {
            this.t = t;
        }

        public TracerConfiguration get() {
            return t;
        }
    }
}
