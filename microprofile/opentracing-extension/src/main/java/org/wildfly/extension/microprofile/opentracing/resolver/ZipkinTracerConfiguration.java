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
package org.wildfly.extension.microprofile.opentracing.resolver;

import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.COMPRESSION;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.ENCODING;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.ENDPOINT;

import brave.Tracing;
import io.opentracing.Tracer;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.microprofile.opentracing.smallrye.TracerConfiguration;
import zipkin2.codec.Encoding;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.okhttp3.OkHttpSender;

import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.CONNECTION_TIMEOUT;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.MAX_REQUEST;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.MESSAGE_MAX_BYTES;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.READ_TIMEOUT;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.SAMPLER;
import static org.wildfly.extension.microprofile.opentracing.TracerAttributes.TRACEID_128BIT;

import brave.sampler.Sampler;

/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class ZipkinTracerConfiguration implements TracerConfiguration {

    private final AsyncReporter spanReporter;
    private final Sampler sampler;
    private final boolean traceId128Bit;

    public ZipkinTracerConfiguration(OperationContext context, ModelNode configuration) throws OperationFailedException {
        spanReporter = AsyncReporter
                .builder(OkHttpSender.newBuilder()
                        .endpoint(ENDPOINT.resolveModelAttribute(context, configuration).asString())
                        .encoding(Encoding.valueOf(ENCODING.resolveModelAttribute(context, configuration).asString()))
                        .connectTimeout(CONNECTION_TIMEOUT.resolveModelAttribute(context, configuration).asInt())
                        .compressionEnabled(COMPRESSION.resolveModelAttribute(context, configuration).asBoolean())
                        .maxRequests(MAX_REQUEST.resolveModelAttribute(context, configuration).asInt())
                        .messageMaxBytes(MESSAGE_MAX_BYTES.resolveModelAttribute(context, configuration).asInt())
                        .readTimeout(READ_TIMEOUT.resolveModelAttribute(context, configuration).asInt())
                        .build())
                .build();
        float sample = Double.valueOf(SAMPLER.resolveModelAttribute(context, configuration).asDouble()).floatValue();
        if (sample == 0F) {
            sampler = Sampler.NEVER_SAMPLE;
        } else if (sample >= 1F) {
            sampler = Sampler.ALWAYS_SAMPLE;
        } else {
            sampler = Sampler.create(sample);
        }
        traceId128Bit = TRACEID_128BIT.resolveModelAttribute(context, configuration).asBoolean();
    }

    @Override
    public Tracer createTracer(String serviceName) {
        return brave.opentracing.BraveTracer.create(
                Tracing.newBuilder()
                        .localServiceName(serviceName)
                        .spanReporter(spanReporter)
                        .sampler(sampler)
                        .traceId128Bit(traceId128Bit)
                        .build());
    }

    @Override
    public String getModuleName() {
        return "io.zipkin.brave";
    }

}
