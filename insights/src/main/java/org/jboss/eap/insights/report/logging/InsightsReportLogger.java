/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.eap.insights.report.logging;

import static org.jboss.logging.Logger.Level.DEBUG;

import com.redhat.insights.InsightsException;
import com.redhat.insights.jars.JarInfo;
import java.nio.file.Path;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Emmanuel Hugonnet (c) 2023 Red Hat, Inc.
 */
@SuppressWarnings({"DefaultAnnotationParam", "deprecation"})
@MessageLogger(projectCode = "WFLYINSIGHTS", length = 4)
public interface InsightsReportLogger extends BasicLogger {

    /**
     * Default root logger with category of the package name.
     */
    InsightsReportLogger ROOT_LOGGER = Logger.getMessageLogger(InsightsReportLogger.class, "org.jboss.eap.insights.report");

    @Message(id = 1, value = "Reading the runtime configuration failed with  %s")
    InsightsException failedToReadRuntimeConfiguration(String failureDescription);

    @Message(id = 2, value = "Reading the runtime configuration failed")
    InsightsException failedToReadRuntimeConfiguration(@Cause Exception cause);

    @Message(id = 3, value = "Analyzing the module paths failed")
    InsightsException failedToReadModules(@Cause Exception cause);

    @Message(id = 4, value = "Error processing deployments")
    InsightsException failedToProcessDeployments(@Cause Exception cause);

    @Message(id = 5, value = "Authentication missing from request: %s")
    InsightsException missingAuthentication(String message);

    @Message(id = 6, value = "Payload too large: %s")
    InsightsException payloadTooLarge(String message);

    @Message(id = 7, value = "Content type of payload is unsupported: %s")
    InsightsException unsupportedContentType(String message);

    @Message(id = 8, value = "Request failed on the server with code: %s")
    InsightsException serversideError(String statusLine);

    @LogMessage(level = DEBUG)
    @Message(id = Message.NONE, value = "Adding the info for %s")
    void addingAnalyzedJar(JarInfo info);

    @LogMessage(level = DEBUG)
    @Message(id = Message.NONE, value = "Analyzing the jar %s failed")
    void errorAnalyzingJar(Path jar, @Cause Exception cause);

    @LogMessage(level = DEBUG)
    @Message(id = Message.NONE, value = "Modules analysis done")
    void endProcessingModules();

    @LogMessage(level = DEBUG)
    @Message(id = Message.NONE, value = "Module path %s is being processed")
    void startProcessingModulePath(Path modulePath);

    @LogMessage(level = DEBUG)
    @Message(id = Message.NONE, value = "Processing deployment %s")
    void startProcessingDeployment(String deploymentName);

    @LogMessage(level = DEBUG)
    @Message(id = Message.NONE, value = "Processing deployment %s for update")
    void startProcessingDeploymentForUpdate(String deploymentName);

    @LogMessage(level = DEBUG)
    @Message(id = Message.NONE, value = "The deployment %s has sha1 hash %s")
    void deploymentHashFound(String deploymentName, String hash);

    @LogMessage(level = DEBUG)
    @Message(id = Message.NONE, value = "All deployments have been processed")
    void endProcessingDeployments();

    @LogMessage(level = DEBUG)
    @Message(id = Message.NONE, value = "All deployments have been updated")
    void endProcessingDeploymentsForUpdate();

    @LogMessage(level = DEBUG)
    @Message(id = Message.NONE, value = "Scheduled send failed")
    void scheduledSendFailed(@Cause Exception cause);

    @LogMessage(level = DEBUG)
    @Message(id = Message.NONE, value = "Non-Insights failure")
    void scheduledSendUnknownException(@Cause Exception cause);
}
