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
package org.wildfly.extension.metrics;


import java.util.Map;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Emmanuel Hugonnet (c) 2023 Red Hat, Inc.
 */
public class ReadMetricsOperation extends AbstractRuntimeOnlyHandler {
    private final WildFlyMetricRegistry registry;

    public ReadMetricsOperation(WildFlyMetricRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected void executeRuntimeStep(OperationContext oc, ModelNode mn) throws OperationFailedException {
        for (Map.Entry<MetricID, Metric> entry : registry.getMetrics().entrySet()) {
            MetricID metricID = entry.getKey();
            String metricName = entry.getKey().getMetricName();
            MetricMetadata metadata = registry.getMetricMetadata().get(metricName);
            String prometheusMetricName = toPrometheusMetricName(metricID, metadata);
            oc.getResult().get(prometheusMetricName).set(((WildFlyMetric) entry.getValue()).getValue(oc));
        }
    }

    private static String toPrometheusMetricName(MetricID metricID, MetricMetadata metadata) {
        String prometheusName = metricID.getMetricName();
        // change the Prometheus name depending on type and measurement unit
        if (metadata.getType() == WildFlyMetricMetadata.Type.COUNTER) {
            prometheusName += "_total";
        } else {
            // if it's a gauge, let's add the base unit to the prometheus name
            String baseUnit = metadata.getBaseMetricUnit();
            if (!MetricMetadata.NONE.equals(baseUnit)) {
                prometheusName += "_" + baseUnit;
            }
        }
        return prometheusName;
    }
    
}
