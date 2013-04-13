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

package org.jboss.as.osgi.deployment;

import static org.jboss.as.server.deployment.Attachments.BUNDLE_STATE_KEY;
import static org.jboss.osgi.framework.spi.IntegrationConstants.STORAGE_STATE_KEY;

import org.jboss.as.osgi.OSGiConstants;
import org.jboss.as.osgi.service.InitialDeploymentTracker;
import org.jboss.as.server.deployment.Attachments.BundleState;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.spi.BundleManager;
import org.jboss.osgi.framework.spi.IntegrationServices;
import org.jboss.osgi.framework.spi.StorageManager;
import org.jboss.osgi.framework.spi.StorageState;
import org.jboss.osgi.resolver.XBundleRevision;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * Processes deployments that have OSGi metadata attached.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 20-Sep-2010
 */
public class BundleInstallProcessor implements DeploymentUnitProcessor {

    private final InitialDeploymentTracker deploymentTracker;

    public BundleInstallProcessor(InitialDeploymentTracker deploymentTracker) {
        this.deploymentTracker = deploymentTracker;
    }

    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit depUnit = phaseContext.getDeploymentUnit();
        final Deployment deployment = depUnit.getAttachment(OSGiConstants.DEPLOYMENT_KEY);
        if (deployment == null)
            return;

        ServiceController<? extends XBundleRevision> controller;
        try {
            BundleManager bundleManager = depUnit.getAttachment(OSGiConstants.BUNDLE_MANAGER_KEY);
            BundleContext syscontext = depUnit.getAttachment(OSGiConstants.SYSTEM_CONTEXT_KEY);
            if (deploymentTracker.hasDeploymentName(depUnit.getName())) {
                restoreStorageState(phaseContext, deployment);
            }
            controller = bundleManager.createBundleRevision(syscontext, deployment, phaseContext.getServiceTarget(), null);
        } catch (BundleException ex) {
            throw new DeploymentUnitProcessingException(ex);
        }

        // Add a dependency on the next phase for this bundle to be installed
        phaseContext.addDeploymentDependency(controller.getName(), OSGiConstants.BUNDLE_REVISION_KEY);
        depUnit.putAttachment(BUNDLE_STATE_KEY, BundleState.INSTALLED);
    }

    @Override
    public void undeploy(final DeploymentUnit depUnit) {
        // When we get here, the {@link BundleRevision} service has already gone down
        // cleaning up all revision state. Uninstall is therfore done in the {@link BundleRevision} service
    }

    private void restoreStorageState(final DeploymentPhaseContext phaseContext, final Deployment deployment) {
        ServiceRegistry serviceRegistry = phaseContext.getServiceRegistry();
        StorageManager storageProvider = (StorageManager) serviceRegistry.getRequiredService(IntegrationServices.STORAGE_MANAGER_PLUGIN).getValue();
        StorageState storageState = storageProvider.getStorageState(deployment.getLocation());
        if (storageState != null) {
            deployment.setAutoStart(storageState.isPersistentlyStarted());
            deployment.putAttachment(STORAGE_STATE_KEY, storageState);
        }
    }
}
