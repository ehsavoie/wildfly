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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.redhat.insights.InsightsSubreport;
import java.io.IOException;

/**
 *
 * @author Emmanuel Hugonnet (c) 2023 Red Hat, Inc.
 */
class JBossInsightsSubReportSerializer extends JsonSerializer<InsightsSubreport> {

    @Override
    public void serialize(InsightsSubreport subReport, JsonGenerator generator, SerializerProvider serializerProvider) throws IOException {
        JBossInsightsSubReport jBossInsightsSubReport = (JBossInsightsSubReport) subReport;
        generator.writeStartObject();
        generator.writeStringField("version", jBossInsightsSubReport.getVersion());
        generator.writeFieldName("configuration");
        generator.writeRaw(" : " +jBossInsightsSubReport.getReport());
        generator.writeEndObject();
        generator.flush();
    }
}
