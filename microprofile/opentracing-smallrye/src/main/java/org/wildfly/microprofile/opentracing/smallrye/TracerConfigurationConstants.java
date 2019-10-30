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

/**
 *
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public class TracerConfigurationConstants {


    public static enum Type {
        JAEGER, ZIPKIN;
    }

    public static final String PROPAGATION = "propagation";

    public static final String SAMPLER_TYPE = "sampler-type";
    public static final String SAMPLER_PARAM = "sampler-param";
    public static final String SAMPLER_MANAGER_HOST_PORT = "sampler-manager-host-port";

    public static final String SENDER_AGENT_HOST = "sender-host";
    public static final String SENDER_AGENT_PORT = "sender-port";
    public static final String SENDER_ENDPOINT = "sender-endpoint";
    public static final String SENDER_AUTH_TOKEN = "sender-auth-token";
    public static final String SENDER_AUTH_USER = "sender-auth-user";
    public static final String SENDER_AUTH_PASSWORD = "sender-auth-password";

    public static final String REPORTER_LOG_SPANS = "reporter-log-spans";
    public static final String REPORTER_FLUSH_INTERVAL = "reporter-flush-interval";
    public static final String REPORTER_MAX_QUEUE_SIZE = "reporter-max-queue-size";

    public static final String TRACEID_128BIT = "tracer_id_128bit";
    public static final String TRACER_TAGS = "tags";


    public static final String COMPRESSION = "compression";
    public static final String CONNECTION_TIMEOUT = "connection-timeout";
    public static final String ENCODING = "encoding";
    public static final String ENDPOINT = "endpoint";
    public static final String MAX_REQUEST = "max-request";
    public static final String MESSAGE_MAX_BYTES = "message-max-bytes";
    public static final String READ_TIMEOUT = "read-timeout";
    public static final String SAMPLER = "sampler";
}
