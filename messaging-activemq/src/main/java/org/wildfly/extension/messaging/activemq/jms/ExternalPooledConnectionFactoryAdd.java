/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq.jms;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_CLUSTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.LOCAL;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.LOCAL_TX;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.NONE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.NO_TX;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.XA_TX;
import static org.wildfly.extension.messaging.activemq.MessagingServices.isSubsystemResource;
import static org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttribute.getDefinitions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.artemis.api.core.TransportConfiguration;

import org.jboss.as.connector.metadata.deployment.ResourceAdapterDeployment;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.deployment.ContextNames.BindInfo;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.DiscoveryGroupDefinition;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.extension.messaging.activemq.TransportConfigOperationHandlers;
import org.wildfly.extension.messaging.activemq.jms.ConnectionFactoryAttributes.Common;

/**
 *
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class ExternalPooledConnectionFactoryAdd extends AbstractAddStepHandler {

    public static final ExternalPooledConnectionFactoryAdd INSTANCE = new ExternalPooledConnectionFactoryAdd();

    private ExternalPooledConnectionFactoryAdd() {
        super(getDefinitions(ExternalPooledConnectionFactoryDefinition.ATTRIBUTES));
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {

        ModelNode model = resource.getModel();
        PathAddress address = context.getCurrentAddress();
        final String name = context.getCurrentAddressValue();

        final ModelNode resolvedModel = model.clone();
        for(final AttributeDefinition attribute : getDefinitions(ExternalPooledConnectionFactoryDefinition.ATTRIBUTES)) {
            resolvedModel.get(attribute.getName()).set(attribute.resolveModelAttribute(context, resolvedModel ));
        }

        // We validated that jndiName part of the model in populateModel
        final List<String> jndiNames = new ArrayList<String>();
        for (ModelNode node : resolvedModel.get(Common.ENTRIES.getName()).asList()) {
            jndiNames.add(node.asString());
        }
        final BindInfo bindInfo = ContextNames.bindInfoFor(jndiNames.get(0));
        List<String> jndiAliases;
        if(jndiNames.size() > 1) {
            jndiAliases = new ArrayList<>(jndiNames.subList(1, jndiNames.size()));
        } else {
            jndiAliases = Collections.emptyList();
        }
        String managedConnectionPoolClassName = null;
        if (resolvedModel.hasDefined(ConnectionFactoryAttributes.Pooled.MANAGED_CONNECTION_POOL.getName())) {
            managedConnectionPoolClassName = resolvedModel.get(ConnectionFactoryAttributes.Pooled.MANAGED_CONNECTION_POOL.getName()).asString();
        }
        final int minPoolSize = resolvedModel.get(ConnectionFactoryAttributes.Pooled.MIN_POOL_SIZE.getName()).asInt();
        final int maxPoolSize = resolvedModel.get(ConnectionFactoryAttributes.Pooled.MAX_POOL_SIZE.getName()).asInt();
        Boolean enlistmentTrace = null;
        if (resolvedModel.hasDefined(ConnectionFactoryAttributes.Pooled.ENLISTMENT_TRACE.getName())) {
            enlistmentTrace = resolvedModel.get(ConnectionFactoryAttributes.Pooled.ENLISTMENT_TRACE.getName()).asBoolean();
        }

        final String txSupport;
        if(resolvedModel.hasDefined(ConnectionFactoryAttributes.Pooled.TRANSACTION.getName())) {
            String txType = resolvedModel.get(ConnectionFactoryAttributes.Pooled.TRANSACTION.getName()).asString();
            if(LOCAL.equals(txType)) {
                txSupport = LOCAL_TX;
            } else if (NONE.equals(txType)) {
                 txSupport = NO_TX;
            } else {
                txSupport = XA_TX;
            }
        } else {
            txSupport = XA_TX;
        }

        List<String> connectors = Common.CONNECTORS.unwrap(context, model);
        String discoveryGroupName = getDiscoveryGroup(resolvedModel);
        String jgroupClusterName = null;
        String jgroupsChannelName = null;
        final PathAddress serverAddress = MessagingServices.getActiveMQServerPathAddress(address);
        if (discoveryGroupName != null) {
            PathAddress dgAddress;
            if(isSubsystemResource(context)) {
                dgAddress = address.getParent().append(CommonAttributes.DISCOVERY_GROUP, discoveryGroupName);
            } else {
                dgAddress = serverAddress.append(CommonAttributes.DISCOVERY_GROUP, discoveryGroupName);
            }
            Resource dgResource = context.readResourceFromRoot(dgAddress, false);
            ModelNode dgModel = dgResource.getModel();
            ModelNode jgroupCluster = JGROUPS_CLUSTER.resolveModelAttribute(context, dgModel);
            if(jgroupCluster.isDefined()) {
                jgroupClusterName = jgroupCluster.asString();
                ModelNode channel = DiscoveryGroupDefinition.JGROUPS_CHANNEL.resolveModelAttribute(context, dgModel);
                if(channel.isDefined()) {
                    jgroupsChannelName = channel.asString();
                }
            }
        }

        List<PooledConnectionFactoryConfigProperties> adapterParams = getAdapterParams(resolvedModel, context);
        DiscoveryGroupConfiguration discoveryGroupConfiguration = null;
        if (discoveryGroupName != null) {
            discoveryGroupConfiguration = ExternalConnectionFactoryAdd.getDiscoveryGroup(context, discoveryGroupName);
        }
        Set<String> connectorsSocketBindings = new HashSet<>();
        TransportConfiguration[] transportConfigurations = TransportConfigOperationHandlers.processConnectors(context, connectors, connectorsSocketBindings);
        ExternalPooledConnectionFactoryService.installService(context, name, transportConfigurations, discoveryGroupConfiguration, connectorsSocketBindings,
                jgroupClusterName, jgroupsChannelName, adapterParams, bindInfo, jndiAliases, txSupport, minPoolSize, maxPoolSize, managedConnectionPoolClassName, enlistmentTrace, model);
        boolean statsEnabled = ConnectionFactoryAttributes.Pooled.STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();

        if (statsEnabled) {
            // Add the stats resource. This is kind of a hack as we are modifying the resource
            // in runtime, but oh well. We don't use readResourceForUpdate for this reason.
            // This only runs in this add op anyway, and because it's an add we know readResource
            // is going to be returning the current write snapshot of the model, i.e. the one we want
            PooledConnectionFactoryStatisticsService.registerStatisticsResources(resource);

            installStatistics(context, name);
        }
    }

    static String getDiscoveryGroup(final ModelNode model) {
        if(model.hasDefined(Common.DISCOVERY_GROUP.getName())) {
            return model.get(Common.DISCOVERY_GROUP.getName()).asString();
        }
        return null;
    }
    static List<PooledConnectionFactoryConfigProperties> getAdapterParams(ModelNode model, OperationContext context) throws OperationFailedException {
        List<PooledConnectionFactoryConfigProperties> configs = new ArrayList<PooledConnectionFactoryConfigProperties>();
        for (ConnectionFactoryAttribute nodeAttribute : ExternalPooledConnectionFactoryDefinition.ATTRIBUTES)
        {
            if (!nodeAttribute.isResourceAdapterProperty())
                continue;

            AttributeDefinition definition = nodeAttribute.getDefinition();
            ModelNode node = definition.resolveModelAttribute(context, model);
            if (node.isDefined()) {
                String attributeName = definition.getName();
                final String value;
                if (attributeName.equals(Common.DESERIALIZATION_BLACKLIST.getName())) {
                    value = String.join(",", Common.DESERIALIZATION_BLACKLIST.unwrap(context, model));
                } else if (attributeName.equals(Common.DESERIALIZATION_WHITELIST.getName())) {
                    value = String.join(",", Common.DESERIALIZATION_WHITELIST.unwrap(context, model));
                } else {
                    value = node.asString();
                }
                configs.add(new PooledConnectionFactoryConfigProperties(nodeAttribute.getPropertyName(), value, nodeAttribute.getClassType(), nodeAttribute.getConfigType()));
            }
        }
        return configs;
    }

    private void installStatistics(OperationContext context, String name) {
        ServiceName raActivatorsServiceName = PooledConnectionFactoryService.getResourceAdapterActivatorsServiceName(name);
        PooledConnectionFactoryStatisticsService statsService = new PooledConnectionFactoryStatisticsService(context.getResourceRegistrationForUpdate(), true);
        context.getServiceTarget().addService(raActivatorsServiceName.append("statistics"), statsService)
                .addDependency(raActivatorsServiceName, ResourceAdapterDeployment.class, statsService.getRADeploymentInjector())
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .install();
    }
}
