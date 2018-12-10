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

package org.jboss.as.test.smoke.messaging.client.messaging;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.jboss.as.controller.client.helpers.ClientConstants.ADD;
import static org.jboss.as.controller.client.helpers.ClientConstants.REMOVE_OPERATION;

/**
 * Demo using the AS management API to create and destroy a Artemis core queue.
 *
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CoreQueueMessagingTestCase {

    private final String queueName = "queue.standalone";
    private final int messagingPort = 5445;

    private final String messagingSocketBindingName = "messaging";
    private final String remoteAcceptorName = "netty";

    @ContainerResource
    private ManagementClient managementClient;

    @Test
    public void testMessagingClientUsingMessagingPort() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        jmsOperations.addCoreQueue(queueName, queueName, true);
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
        jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        jmsOperations.removeCoreQueue(queueName);
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
        jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        jmsOperations.addCoreQueue(queueName, queueName, true);
    }

    @Before
    public void setup() throws Exception {
        createSocketBinding(managementClient.getControllerClient(), messagingSocketBindingName, messagingPort);
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        jmsOperations.createRemoteAcceptor(remoteAcceptorName, messagingSocketBindingName, null);
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }


    @After
    public void tearDown() throws Exception {
        JMSOperations jmsOperations = JMSOperationsProvider.getInstance(managementClient.getControllerClient());
        jmsOperations.removeCoreQueue(queueName);
        jmsOperations.removeRemoteAcceptor(remoteAcceptorName);
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
        removeSocketBinding(managementClient.getControllerClient(), messagingSocketBindingName);
        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    public final void createSocketBinding(final ModelControllerClient modelControllerClient, final String name, int port) {
        ModelNode model = new ModelNode();
        model.get(ClientConstants.OP).set(ADD);
        model.get(ClientConstants.OP_ADDR).add("socket-binding-group", "standard-sockets");
        model.get(ClientConstants.OP_ADDR).add("socket-binding", name);
        model.get("interface").set("public");
        model.get("port").set(port);
        model.get(ModelDescriptionConstants.OPERATION_HEADERS).get("allow-resource-service-restart").set(true);

        try {
            execute(modelControllerClient, model);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public final void removeSocketBinding(final ModelControllerClient modelControllerClient, final String name) {
        ModelNode model = new ModelNode();
        model.get(ClientConstants.OP).set(REMOVE_OPERATION);
        model.get(ClientConstants.OP_ADDR).add("socket-binding-group", "standard-sockets");
        model.get(ClientConstants.OP_ADDR).add("socket-binding", name);
        model.get(ModelDescriptionConstants.OPERATION_HEADERS).get("allow-resource-service-restart").set(true);
        try {
            execute(modelControllerClient, model);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes the operation and returns the result if successful. Else throws an exception
     *
     * @param modelControllerClient
     * @param operation
     * @return
     * @throws IOException
     */
    private ModelNode execute(final ModelControllerClient modelControllerClient, final ModelNode operation) throws IOException {
        final ModelNode result = modelControllerClient.execute(operation);
        if (result.hasDefined(ClientConstants.OUTCOME) && ClientConstants.SUCCESS.equals(result.get(ClientConstants.OUTCOME).asString())) {
            //logger.trace("Operation " + operation.toString() + " successful");
            return result;
        } else if (result.hasDefined(ClientConstants.FAILURE_DESCRIPTION)) {
            final String failureDesc = result.get(ClientConstants.FAILURE_DESCRIPTION).toString();
            throw new RuntimeException(failureDesc);
        } else {
            throw new RuntimeException("Operation not successful; outcome = " + result.get(ClientConstants.OUTCOME));
        }
    }
}
