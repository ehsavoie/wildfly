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
package org.jboss.eap.insights.report.logging;

import static org.jboss.eap.insights.report.logging.InsightsReportLogger.ROOT_LOGGER;

import com.redhat.insights.logging.InsightsLogger;

/**
 * @author Emmanuel Hugonnet (c) 2023 Red Hat, Inc.
 */
public class JbossLoggingInsightsLogger implements InsightsLogger {

    public static final InsightsLogger INSTANCE = new JbossLoggingInsightsLogger();

    @Override
    public void debug(String string) {
        ROOT_LOGGER.debug(string);
    }

    @Override
    public void debug(String string, Throwable thrwbl) {
        ROOT_LOGGER.debug(string, thrwbl);
    }

    @Override
    public void info(String string) {
        ROOT_LOGGER.info(string);
    }

    @Override
    public void error(String string) {
        ROOT_LOGGER.error(string);
    }

    @Override
    public void error(String string, Throwable thrwbl) {
        ROOT_LOGGER.error(string, thrwbl);
    }

    @Override
    public void warning(String string) {
        ROOT_LOGGER.warn(string);
    }

    @Override
    public void warning(String string, Throwable thrwbl) {
        ROOT_LOGGER.warn(string, thrwbl);
    }

}
