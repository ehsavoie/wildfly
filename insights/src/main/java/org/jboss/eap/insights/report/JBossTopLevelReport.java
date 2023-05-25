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

import com.redhat.insights.AbstractTopLevelReportBase;
import com.redhat.insights.InsightsException;
import com.redhat.insights.InsightsSubreport;
import com.redhat.insights.config.InsightsConfiguration;
import com.redhat.insights.logging.InsightsLogger;
import java.util.Map;

/**
 * Base class that collects basic information and delegates to the subreport for
 * product-specific details.
 *
 * <p>
 * Products that want to provide Insights reports will subclass this class and
 * also provide a subreport class and a serializer.
 */
class JBossTopLevelReport extends AbstractTopLevelReportBase {

    private final String identificationName;

    JBossTopLevelReport(InsightsLogger logger, InsightsConfiguration config, Map<String, InsightsSubreport> subReports, String identificationName) {
        super(logger, config, subReports);
        this.identificationName = identificationName;
    }

    @Override
    protected long getProcessPID() {
        return Long.parseLong(java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
    }

    @Override
    protected String getIdentificationName() {
        try {
            return super.getIdentificationName();
        } catch (InsightsException ex) {
            return identificationName;
        }
    }

    @Override
    protected Package[] getPackages() {
        return Package.getPackages();
    }
}
