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

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleMapAttributeDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.microprofile.opentracing.smallrye.TracerConfigurationConstants;

/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class TracerAttributes {

    public static final StringListAttributeDefinition PROPAGATION = StringListAttributeDefinition.Builder.of(TracerConfigurationConstants.PROPAGATION)
            .setAllowNullElement(false)
            .setRequired(false)
            .setAllowedValues("JAEGER", "B3")
            .setAttributeGroup("codec-configuration")
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition SAMPLER_TYPE = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.SAMPLER_TYPE, ModelType.STRING, true)
            .setAllowedValues("const", "probabilistic", "ratelimiting", "remote")
            .setAttributeGroup("sampler-configuration")
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition SAMPLER_PARAM = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.SAMPLER_PARAM, ModelType.DOUBLE, true)
            .setAttributeGroup("sampler-configuration")
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition SAMPLER_MANAGER_HOST_PORT = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.SAMPLER_MANAGER_HOST_PORT, ModelType.STRING, true)
            .setAttributeGroup("sampler-configuration")
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition SENDER_AGENT_HOST = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.SENDER_AGENT_HOST, ModelType.STRING, true)
            .setAttributeGroup("sender-configuration")
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition SENDER_AGENT_PORT = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.SENDER_AGENT_PORT, ModelType.INT, true)
            .setAttributeGroup("sender-configuration")
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition SENDER_ENDPOINT = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.SENDER_ENDPOINT, ModelType.STRING, true)
            .setAttributeGroup("sender-configuration")
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition SENDER_AUTH_TOKEN = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.SENDER_AUTH_TOKEN, ModelType.STRING, true)
            .setAttributeGroup("sender-configuration")
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition SENDER_AUTH_USER = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.SENDER_AUTH_USER, ModelType.STRING, true)
            .setAttributeGroup("sender-configuration")
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition SENDER_AUTH_PASSWORD = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.SENDER_AUTH_PASSWORD, ModelType.STRING, true)
            .setAttributeGroup("sender-configuration")
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition REPORTER_LOG_SPANS = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.REPORTER_LOG_SPANS, ModelType.BOOLEAN, true)
            .setAttributeGroup("reporter-configuration")
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition REPORTER_FLUSH_INTERVAL = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.REPORTER_FLUSH_INTERVAL, ModelType.INT, true)
            .setAttributeGroup("reporter-configuration")
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition REPORTER_MAX_QUEUE_SIZE = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.REPORTER_MAX_QUEUE_SIZE, ModelType.INT, true)
            .setAttributeGroup("reporter-configuration")
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition TRACEID_128BIT = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.TRACEID_128BIT, ModelType.BOOLEAN, true)
            .setDefaultValue(ModelNode.FALSE)
            .setRestartAllServices()
            .build();

    public static final SimpleMapAttributeDefinition TRACER_TAGS = new SimpleMapAttributeDefinition.Builder(TracerConfigurationConstants.TRACER_TAGS, ModelType.STRING, true)
            .setRestartAllServices()
            .build();

//    public static ObjectTypeAttributeDefinition JAEGER_CONFIGURATION = ObjectTypeAttributeDefinition
//            .create("jaeger", PROPAGATION, SAMPLER_TYPE, SAMPLER_PARAM, SAMPLER_MANAGER_HOST_PORT, SENDER_AGENT_HOST,
//                SENDER_AGENT_PORT, SENDER_ENDPOINT, SENDER_AUTH_TOKEN, SENDER_AUTH_USER, SENDER_AUTH_PASSWORD,
//                REPORTER_LOG_SPANS, REPORTER_FLUSH_INTERVAL, REPORTER_MAX_QUEUE_SIZE, TRACEID_128BIT)
//            .setAlternatives("zipkin")
//            .setRestartAllServices()
//            .setRequired(false)
//            .build();
//
//    public static ObjectTypeAttributeDefinition ZIPKIN_CONFIGURATION = ObjectTypeAttributeDefinition
//            .create("zipkin", SENDER_ENDPOINT, TRACEID_128BIT)
//            .setAlternatives("jaeger")
//            .setRestartAllServices()
//            .setRequired(false)
//            .build();
    public static final SimpleAttributeDefinition ENDPOINT = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.ENDPOINT, ModelType.STRING, false)
            .setAttributeGroup("sender-configuration")
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition ENCODING = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.ENCODING, ModelType.STRING, true)
            .setAllowedValues("JSON", "THRIFT", "PROTO3")
            .setDefaultValue(new ModelNode("JSON"))
            .setAttributeGroup("reporter-configuration")
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition CONNECTION_TIMEOUT = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.CONNECTION_TIMEOUT, ModelType.INT, true)
            .setDefaultValue(new ModelNode(10000))
            .setMeasurementUnit(MeasurementUnit.MICROSECONDS)
            .setAttributeGroup("reporter-configuration")
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition READ_TIMEOUT = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.READ_TIMEOUT, ModelType.INT, true)
            .setDefaultValue(new ModelNode(10000))
            .setMeasurementUnit(MeasurementUnit.MICROSECONDS)
            .setAttributeGroup("reporter-configuration")
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition MAX_REQUEST = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.MAX_REQUEST, ModelType.INT, true)
            .setDefaultValue(new ModelNode(64))
            .setAttributeGroup("reporter-configuration")
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition MESSAGE_MAX_BYTES = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.MESSAGE_MAX_BYTES, ModelType.INT, true)
            .setDefaultValue(new ModelNode(5 * 1024 * 1024))
            .setMeasurementUnit(MeasurementUnit.BYTES)
            .setAttributeGroup("reporter-configuration")
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition COMPRESSION = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.COMPRESSION, ModelType.BOOLEAN, true)
            .setDefaultValue(ModelNode.TRUE)
            .setAttributeGroup("reporter-configuration")
            .setRestartAllServices()
            .build();
    public static final SimpleAttributeDefinition SAMPLER = SimpleAttributeDefinitionBuilder.create(TracerConfigurationConstants.SAMPLER, ModelType.DOUBLE, true)
            .setDefaultValue(new ModelNode(1.0D))
            .setAttributeGroup("sampler-configuration")
            .setRestartAllServices()
            .build();
}
