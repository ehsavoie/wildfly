/*
 * Copyright 2023 JBoss by Red Hat.
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
package org.jboss.eap.insights.report;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CAPABILITY_REGISTRY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CORE_SERVICE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MAJOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MICRO_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_MINOR_VERSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PLATFORM_MBEAN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.redhat.insights.InsightsSubreport;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Executor;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.PathAddress;
import static org.jboss.as.controller.PathAddress.pathAddress;
import org.jboss.as.controller.PathElement;
import static org.jboss.as.controller.PathElement.WILDCARD_VALUE;
import static org.jboss.as.controller.PathElement.pathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ACCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.AUTHORIZATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROVIDER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.XML_NAMESPACES;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.eap.insights.report.logging.InsightsReportLogger;

/**
 *
 * @author Emmanuel Hugonnet (c) 2023 Red Hat, Inc.
 */
class JBossInsightsSubReport implements InsightsSubreport {

    private final ModelControllerClient modelControllerClient;
    private ModelNode report;
    private String version;

    JBossInsightsSubReport(final ModelControllerClientFactory clientFactory, final Executor executor) {
        this.modelControllerClient = clientFactory.createSuperUserClient(executor, false);
    }

    @Override
    public void generateReport() {
        try {
            ModelNode op = Operations.createReadResourceOperation(PathAddress.EMPTY_ADDRESS.toModelNode(), true);
            op.get(OPERATION_HEADERS).get(ROLES).add("Monitor");
            op.get("include-runtime").set(true);
            op.get("include-defaults").set(true);
            ModelNode response = this.modelControllerClient.execute(op);
            report = new ModelNode("report");
            if (SUCCESS.equals(response.get(OUTCOME).asString())) {
                ModelNode configuration = response.get(RESULT);
                ModelNode provider = configuration.get(CORE_SERVICE, MANAGEMENT, ACCESS, AUTHORIZATION, PROVIDER).clone();
                configuration = prune(configuration, pathAddress(CORE_SERVICE, PLATFORM_MBEAN));
                configuration = prune(configuration, pathAddress(CORE_SERVICE, CAPABILITY_REGISTRY));
                configuration = prune(configuration, pathAddress(SYSTEM_PROPERTIES, WILDCARD_VALUE));
                configuration = prune(configuration, pathAddress(SYSTEM_PROPERTY, WILDCARD_VALUE));
                configuration = prune(configuration, pathAddress(pathElement(EXTENSION, WILDCARD_VALUE), pathElement(SUBSYSTEM, WILDCARD_VALUE), pathElement(XML_NAMESPACES, WILDCARD_VALUE)));
                configuration = prune(configuration, pathAddress(pathElement(CORE_SERVICE, MANAGEMENT), pathElement(ACCESS, AUTHORIZATION)));
                configuration.get(CORE_SERVICE, MANAGEMENT, ACCESS, AUTHORIZATION, PROVIDER).set(provider);
                version = configuration.get(MANAGEMENT_MAJOR_VERSION).asInt(1) + "." + configuration.get(MANAGEMENT_MINOR_VERSION).asInt(0) + "." + configuration.get(MANAGEMENT_MICRO_VERSION).asInt(0);
                report.set(configuration);
            } else {
                throw InsightsReportLogger.ROOT_LOGGER.failedToReadRuntimeConfiguration(response.get(FAILURE_DESCRIPTION).asString());
            }
        } catch (IOException ex) {
            throw InsightsReportLogger.ROOT_LOGGER.failedToReadRuntimeConfiguration(ex);
        }
    }

    public String getReport() {
        return report.toJSONString(false);
    }

    private ModelNode prune(ModelNode configuration, PathAddress address) {
        Iterator<PathElement> iter = address.iterator();
        int i = 0;
        ModelNode current = configuration;
        while (iter.hasNext()) {
            PathElement element = iter.next();
            i++;
            if (iter.hasNext()) {
                if(element.isWildcard() && current.hasDefined(element.getKey())) {
                    ModelNode children = current.get(element.getKey());
                    for(Property child : children.asPropertyList()) {
                        ModelNode result = prune(child.getValue().clone(), address.subAddress(i ));
                        current.get(element.getKey()).get(child.getName()).set(result);
                    }
                } else {
                   current = current.get(element.getKey()).get(element.getValue());
                }
            } else {
                if (element.isWildcard()) {
                    current.remove(element.getKey());
                } else {
                    current.get(element.getKey()).remove(element.getValue());
                }
            }
        }
        return configuration;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public JsonSerializer<InsightsSubreport> getSerializer() {
        return new JBossInsightsSubReportSerializer();
    }
}
