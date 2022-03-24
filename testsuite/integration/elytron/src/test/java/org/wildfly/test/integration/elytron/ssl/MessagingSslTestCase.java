/*
 * Copyright 2022 JBoss by Red Hat.
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
package org.wildfly.test.integration.elytron.ssl;

import static java.security.AccessController.doPrivileged;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static org.jboss.as.test.shared.ServerReload.reloadIfRequired;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import org.codehaus.plexus.util.FileUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.common.jms.JMSOperations;
import org.jboss.as.test.integration.common.jms.JMSOperationsProvider;
import org.jboss.as.test.integration.security.common.CoreUtils;
import org.jboss.as.test.integration.security.common.SecurityTestConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.ElytronXmlParser;
import org.wildfly.security.auth.client.InvalidAuthenticationConfigurationException;
import org.wildfly.test.security.common.AbstractElytronSetupTask;
import org.wildfly.test.security.common.elytron.ConfigurableElement;
import org.wildfly.test.security.common.elytron.CredentialReference;
import org.wildfly.test.security.common.elytron.Path;
import org.wildfly.test.security.common.elytron.SimpleKeyManager;
import org.wildfly.test.security.common.elytron.SimpleKeyStore;
import org.wildfly.test.security.common.elytron.SimpleServerSslContext;
import org.wildfly.test.security.common.elytron.SimpleTrustManager;

/**
 *
 * @author Emmanuel Hugonnet (c) 2021 Red Hat, Inc.
 */
@RunWith(Arquillian.class)
@ServerSetup({MessagingSslTestCase.ElytronSslContextInArtemisSetupTask.class})
@RunAsClient
public class MessagingSslTestCase {

    private static final String NAME = "artemis";
    private static final String CONNECTION_FACTORY_JNDI_NAME = "java:jboss/exported/jms/RemoteTestConnectionFactory";
    private static final String NETTY_SOCKET_BINDING = "messaging-group";
    private static final int PORT_ARTEMIS_NETTY = 5445;
    private static final String QUEUE_NAME = "createdTestQueue";
    private static final String EXPORTED_QUEUE_NAME = "java:jboss/exported/jms/queue/createdTestQueue";

    private static final File WORK_DIR = new File("target" + File.separatorChar + NAME);
    private static final File SERVER_KEYSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_KEYSTORE);
    private static final File SERVER_TRUSTSTORE_FILE = new File(WORK_DIR, SecurityTestConstants.SERVER_TRUSTSTORE);
    private static final String PASSWORD = SecurityTestConstants.KEYSTORE_PASSWORD;

    @ContainerResource
    private Context remoteContext;

    // just to make server setup task work
    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, NAME + ".war")
                .add(new StringAsset("index page"), "index.html");
    }

    @Test
    public void testResteasyElytronClientTrustedServer() throws Exception {
        AuthenticationContext context = doPrivileged((PrivilegedAction<AuthenticationContext>) () -> {
            try {
                URL config = getClass().getResource("wildfly-config-correct-truststore.xml");
                return ElytronXmlParser.parseAuthenticationClientConfiguration(config.toURI()).create();
            } catch (Throwable t) {
                throw new InvalidAuthenticationConfigurationException(t);
            }
        });
        context.run(() -> {
            try {
                doSendAndReceive();
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    private void doSendAndReceive() throws Exception {
        ConnectionFactory cf = (ConnectionFactory) remoteContext.lookup("jms/RemoteTestConnectionFactory");
        assertNotNull(cf);
        Destination destination = (Destination) remoteContext.lookup("jms/queue/createdTestQueue");
        assertNotNull(destination);

        try ( Connection conn = cf.createConnection("guest", "guest");) {
            conn.start();
            Session consumerSession = conn.createSession(false, AUTO_ACKNOWLEDGE);

            final CountDownLatch latch = new CountDownLatch(10);
            final List<String> result = new ArrayList<>();

            // Set the async listener
            MessageConsumer consumer = consumerSession.createConsumer(destination);
            consumer.setMessageListener(new MessageListener() {

                @Override
                public void onMessage(Message message) {
                    TextMessage msg = (TextMessage) message;
                    try {
                        result.add(msg.getText());
                        latch.countDown();
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }
            });

            final Session producerSession = conn.createSession(false, AUTO_ACKNOWLEDGE);
            MessageProducer producer = producerSession.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            for (int i = 0; i < 10; i++) {
                String s = "Test" + i;
                TextMessage msg = producerSession.createTextMessage(s);
                //System.out.println("sending " + s);
                producer.send(msg);
            }

            producerSession.close();

            assertTrue(latch.await(3, SECONDS));
            assertEquals(10, result.size());
            for (int i = 0; i < result.size(); i++) {
                assertEquals("Test" + i, result.get(i));
            }
        }
    }

    /**
     * Creates Elytron server-ssl-context and key/trust stores.
     */
    static class ElytronSslContextInArtemisSetupTask extends AbstractElytronSetupTask {

        private static final Logger LOGGER = Logger.getLogger(ElytronSslContextInArtemisSetupTask.class);

        @Override
        protected void setup(final ModelControllerClient modelControllerClient) throws Exception {
            LOGGER.warn("***************************** Creating the keys !!!!!!!");
            System.out.println("***************************** Creating the keys !!!!!!!");
            keyMaterialSetup(WORK_DIR);
            super.setup(modelControllerClient);
            JMSOperations jmsAdminOperations = JMSOperationsProvider.getInstance(modelControllerClient);
            jmsAdminOperations.createSocketBinding(NETTY_SOCKET_BINDING, "public", PORT_ARTEMIS_NETTY);
            HashMap<String, String> connectorParams = new HashMap<>();
            connectorParams.put("verifyHost", "false");
            jmsAdminOperations.createRemoteConnector("netty", NETTY_SOCKET_BINDING, NAME, connectorParams);
            try {
                jmsAdminOperations.removeRemoteAcceptor("netty");
            } catch (RuntimeException e) {
            }
            jmsAdminOperations.createRemoteAcceptor("netty", NETTY_SOCKET_BINDING, NAME, null);
            ModelNode attributes = new ModelNode();
            attributes.get("connectors").add("netty");
            jmsAdminOperations.addJmsConnectionFactory("RemoteTestConnectionFactory", CONNECTION_FACTORY_JNDI_NAME, attributes);
            jmsAdminOperations.createJmsQueue(QUEUE_NAME, EXPORTED_QUEUE_NAME);
            reloadIfRequired(modelControllerClient);
        }

        @Override
        protected ConfigurableElement[] getConfigurableElements() {
            return new ConfigurableElement[]{
                SimpleKeyStore.builder().withName(NAME + SecurityTestConstants.SERVER_KEYSTORE)
                .withPath(Path.builder().withPath(SERVER_KEYSTORE_FILE.getPath()).build())
                .withCredentialReference(CredentialReference.builder().withClearText(PASSWORD).build())
                .build(),
                SimpleKeyStore.builder().withName(NAME + SecurityTestConstants.SERVER_TRUSTSTORE)
                .withPath(Path.builder().withPath(SERVER_TRUSTSTORE_FILE.getPath()).build())
                .withCredentialReference(CredentialReference.builder().withClearText(PASSWORD).build())
                .build(),
                SimpleKeyManager.builder().withName(NAME)
                .withKeyStore(NAME + SecurityTestConstants.SERVER_KEYSTORE)
                .withCredentialReference(CredentialReference.builder().withClearText(PASSWORD).build())
                .build(),
                SimpleTrustManager.builder().withName(NAME)
                .withKeyStore(NAME + SecurityTestConstants.SERVER_TRUSTSTORE)
                .build(),
                SimpleServerSslContext.builder().withName(NAME).withKeyManagers(NAME).withTrustManagers(NAME).build()
            };
        }

        @Override
        protected void tearDown(ModelControllerClient modelControllerClient) throws Exception {
            super.tearDown(modelControllerClient);
            JMSOperations jmsAdminOperations = JMSOperationsProvider.getInstance(modelControllerClient);
            jmsAdminOperations.removeJmsQueue(QUEUE_NAME);
            jmsAdminOperations.removeJmsConnectionFactory("RemoteTestConnectionFactory");
            jmsAdminOperations.removeRemoteAcceptor("netty");
            jmsAdminOperations.removeRemoteConnector("netty");
            jmsAdminOperations.removeSocketBinding(NETTY_SOCKET_BINDING);
//            FileUtils.deleteDirectory(WORK_DIR);
        }

        protected static void keyMaterialSetup(File workDir) throws Exception {
            FileUtils.deleteDirectory(workDir);
            workDir.mkdirs();
            Assert.assertTrue(workDir.exists());
            Assert.assertTrue(workDir.isDirectory());
            LOGGER.warn("Creating the keys !!!!!!!");
            CoreUtils.createKeyMaterial(workDir);
        }
    }
}
