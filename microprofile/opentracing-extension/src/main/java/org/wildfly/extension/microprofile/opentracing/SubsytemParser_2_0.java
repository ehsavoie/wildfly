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
package org.wildfly.extension.microprofile.opentracing;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.COMPRESSION;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.CONNECTION_TIMEOUT;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.ENCODING;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.ENDPOINT;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.MAX_REQUEST;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.MESSAGE_MAX_BYTES;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.PROPAGATION;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.READ_TIMEOUT;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.REPORTER_FLUSH_INTERVAL;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.REPORTER_LOG_SPANS;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.REPORTER_MAX_QUEUE_SIZE;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SAMPLER;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SAMPLER_MANAGER_HOST_PORT;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SAMPLER_PARAM;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SAMPLER_TYPE;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SENDER_AGENT_HOST;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SENDER_AGENT_PORT;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SENDER_AUTH_PASSWORD;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SENDER_AUTH_TOKEN;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SENDER_AUTH_USER;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SENDER_ENDPOINT;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.TRACEID_128BIT;

import org.jboss.as.controller.PersistentResourceXMLDescription.PersistentResourceXMLBuilder;

public class SubsytemParser_2_0 extends PersistentResourceXMLParser {

    public static final String NAMESPACE = "urn:wildfly:microprofile-opentracing-smallrye:2.0";

    static final PersistentResourceXMLParser INSTANCE = new SubsytemParser_2_0();

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        PersistentResourceXMLBuilder jaegerTracer = builder(JaegerTracerConfigurationDefinition.TRACER_CONFIGURATION_PATH)
                .addAttributes(
                        PROPAGATION, SAMPLER_TYPE, SAMPLER_PARAM, SAMPLER_MANAGER_HOST_PORT,
                        SENDER_AGENT_HOST, SENDER_AGENT_PORT, SENDER_ENDPOINT, SENDER_AUTH_TOKEN,
                        SENDER_AUTH_USER, SENDER_AUTH_PASSWORD, REPORTER_LOG_SPANS,
                        REPORTER_FLUSH_INTERVAL, REPORTER_MAX_QUEUE_SIZE, TRACEID_128BIT
                );
        PersistentResourceXMLBuilder zipkinTracer = builder(ZipkinTracerConfigurationDefinition.TRACER_CONFIGURATION_PATH)
                .addAttributes(
                        ENDPOINT, ENCODING, CONNECTION_TIMEOUT, READ_TIMEOUT, MAX_REQUEST, MESSAGE_MAX_BYTES,
                        COMPRESSION, SAMPLER
                );
        return builder(SubsystemExtension.SUBSYSTEM_PATH, NAMESPACE)
                .addChild(jaegerTracer)
                .addAttributes()
                .build();
    }
}
