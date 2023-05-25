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

import com.fasterxml.jackson.databind.JsonSerializer;
import com.redhat.insights.InsightsSubreport;
import org.jboss.as.version.ProductConfig;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;

/**
 *
 * @author Emmanuel Hugonnet (c) 2023 Red Hat, Inc.
 */
class JBossInsightsWrapperSubReport implements InsightsSubreport {

    private final JBossInsightsSubReport jBossInsightsSubReport;
    private final JBossJarInfoModuleSubReport jBossJarInfoModuleSubReport;
    private final JBossInsightsDeploymentSubReport jBossInsightsDeploymentSubReport;
    private final ProductConfig productConfig;
    private final boolean useYaml;
    private final boolean bootableJar;
    private final boolean useGit;

    JBossInsightsWrapperSubReport(ProductConfig productConfig,
            JBossInsightsSubReport jBossInsightsSubReport,
            JBossJarInfoModuleSubReport jBossJarInfoModuleSubReport,
            JBossInsightsDeploymentSubReport jBossInsightsDeploymentSubReport,
            boolean useYaml, boolean useGit) {
        this.jBossInsightsSubReport = jBossInsightsSubReport;
        this.jBossJarInfoModuleSubReport = jBossJarInfoModuleSubReport;
        this.jBossInsightsDeploymentSubReport = jBossInsightsDeploymentSubReport;
        this.productConfig = productConfig;
        this.useYaml = useYaml;
        this.useGit= useGit;
        String classPath = System.getProperty("java.class.path");
        bootableJar = classPath != null && ! classPath.contains("jboss-modules.jar");
    }

    public boolean isUseGit() {
        return useGit;
    }

    public boolean isUseYaml() {
        return useYaml;
    }

    public boolean isBootableJar() {
        return bootableJar;
    }

    @Override
    public void generateReport() {
        this.jBossInsightsSubReport.generateReport();
        this.jBossJarInfoModuleSubReport.generateReport();
        this.jBossInsightsDeploymentSubReport.generateReport();
    }

    public JBossInsightsSubReport getWildFlyInsightsSubReport() {
        return jBossInsightsSubReport;
    }

    public JBossJarInfoModuleSubReport getWildFlyJarInfoModuleSubReport() {
        return jBossJarInfoModuleSubReport;
    }

    public JBossInsightsDeploymentSubReport getWildFlyInsightsDeploymentSubReport() {
        return jBossInsightsDeploymentSubReport;
    }

    public String getProduct() {
        return "Red Hat " + productConfig.getProductName();
    }

    public String getProductVersion() {
        return productConfig.getPrettyVersionString();
    }

    public boolean isXP() {
        try {
            Module.getBootModuleLoader().loadModule("org.jboss.eap.expansion.pack");
            return true;
        } catch (ModuleLoadException ex) {
            return false;
        }
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public JsonSerializer<InsightsSubreport> getSerializer() {
        return new JBossInsightsWrapperSubReportSerializer();
    }
}
