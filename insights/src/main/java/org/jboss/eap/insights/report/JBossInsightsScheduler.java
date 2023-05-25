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

import com.redhat.insights.InsightsException;
import com.redhat.insights.InsightsScheduler;
import com.redhat.insights.config.InsightsConfiguration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.jboss.eap.insights.report.logging.InsightsReportLogger;

/**
 *
 * @author Emmanuel Hugonnet (c) 2023 Red Hat, Inc.
 */
class JBossInsightsScheduler implements InsightsScheduler {

    private final ScheduledExecutorService schedulerExecutorService;
    private final InsightsConfiguration configuration;
    private ScheduledFuture<?> connectFuture = null;
    private ScheduledFuture<?> updateFuture = null;

    JBossInsightsScheduler(java.util.concurrent.ScheduledExecutorService schedulerExecutorService, InsightsConfiguration configuration) {
        this.schedulerExecutorService = schedulerExecutorService;
        this.configuration = configuration;
    }

    @Override
    public ScheduledFuture<?> scheduleConnect(Runnable command) {
        connectFuture = scheduleAtFixedRate(command, 0, configuration.getConnectPeriod().getSeconds(), TimeUnit.SECONDS);
        return connectFuture;
    }

    @Override
    public ScheduledFuture<?> scheduleJarUpdate(Runnable command) {
        updateFuture = scheduleAtFixedRate(command, configuration.getUpdatePeriod().getSeconds(),
                configuration.getUpdatePeriod().getSeconds(), TimeUnit.SECONDS);
        return updateFuture;
    }

    private ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        Runnable wrapped = () -> {
            try {
                command.run();
            } catch (InsightsException ix) {
                InsightsReportLogger.ROOT_LOGGER.scheduledSendFailed(ix);
                shutdown();
                throw ix;
            } catch (Exception x) {
                InsightsReportLogger.ROOT_LOGGER.scheduledSendUnknownException(x);
                shutdown();
                throw x;
            }
        };

        return this.schedulerExecutorService.scheduleAtFixedRate(wrapped, initialDelay, period, unit);
    }

    @Override
    public boolean isShutdown() {
        return schedulerExecutorService.isShutdown();
    }

    @Override
    public void shutdown() {
        InsightsReportLogger.ROOT_LOGGER.debug("Scheduler shutdown() invoked. Cancelling running tasks for UPDATE and CONNECT");
        if(updateFuture != null && !updateFuture.isDone()) {
            updateFuture.cancel(true);
        }
        if(connectFuture != null && !connectFuture.isDone()) {
            connectFuture.cancel(true);
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        return schedulerExecutorService.shutdownNow();
    }

}
