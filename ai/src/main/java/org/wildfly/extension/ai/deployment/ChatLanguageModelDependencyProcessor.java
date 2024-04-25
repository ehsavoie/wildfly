/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.ai.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

/**
 *
 * @author Emmanuel Hugonnet (c) 2024 Red Hat, Inc.
 */
public class ChatLanguageModelDependencyProcessor implements DeploymentUnitProcessor {

    public static final String[] MODULES = {
        "dev.langchain4j.openai"
    };

    public static final String[] EXPORTED_MODULES = {
        "dev.langchain4j",
        "org.wildfly.extension.ai",
        "io.opentracing.contrib.opentracing-interceptors"
    };

    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();
        ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        ModuleLoader moduleLoader = Module.getBootModuleLoader();
        for (String module : MODULES) {
            moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, module, false, false, true, false));
        }
        for (String module : EXPORTED_MODULES) {
            ModuleDependency modDep = new ModuleDependency(moduleLoader, module, false, true, true, false);
            modDep.addImportFilter(s -> s.equals("META-INF"), true);
            moduleSpecification.addSystemDependency(modDep);
        }
    }

}
