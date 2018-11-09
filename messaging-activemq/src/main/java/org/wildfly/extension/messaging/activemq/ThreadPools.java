/*
 * Copyright 2018 JBoss by Red Hat.
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
package org.wildfly.extension.messaging.activemq;


import java.util.function.Function;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.threads.ScheduledThreadPoolResourceDefinition;
import org.jboss.as.threads.ThreadFactoryResolver;
import org.jboss.as.threads.ThreadFactoryResourceDefinition;
import org.jboss.as.threads.UnboundedQueueThreadPoolResourceDefinition;

/**
 *
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class ThreadPools {

    public static final String GLOBAL_CLIENT = "global-client";
    private static class ThreadPoolCapabilityNameMapper implements Function<PathAddress,String[]> {
        private static final ThreadPoolCapabilityNameMapper INSTANCE = new ThreadPoolCapabilityNameMapper();

        private ThreadPoolCapabilityNameMapper(){};

        @Override
        public String[] apply(PathAddress address) {
            String[] result = new String[2];
            PathAddress serverAddress = MessagingServices.getActiveMQServerPathAddress(address);
            if(serverAddress.size() > 0 ) {
                result[0] = serverAddress.getLastElement().getValue();
            } else {
                result[0] = GLOBAL_CLIENT;
            }
            result[1] = address.getLastElement().getValue();
            return result;
        }
    }

    public static final PathElement JOURNAl_THREAD_POOL_PATH = PathElement.pathElement(CommonAttributes.JOURNAL_THREAD_POOL);
    public static final PathElement THREAD_POOL_PATH = PathElement.pathElement(CommonAttributes.THREAD_POOL);
    public static final PathElement SCHEDULED_THREAD_POOL_PATH = PathElement.pathElement(CommonAttributes.SCHEDULED_THREAD_POOL);
    public static final PathElement THREAD_FACTORY_PATH = PathElement.pathElement(CommonAttributes.THREAD_FACTORY);

    public static final RuntimeCapability<Void> SCHEDULED_THREAD_POOL_CAPABILITY =
            RuntimeCapability.Builder.of("org.wildfly.threads.executor.messaging-activemq." + CommonAttributes.SCHEDULED_THREAD_POOL,
                    true,
                    ScheduledThreadPoolResourceDefinition.CAPABILITY.getCapabilityServiceValueType())
                    .setDynamicNameMapper(ThreadPoolCapabilityNameMapper.INSTANCE)
                    .build();

    public static final RuntimeCapability<Void> THREAD_POOL_CAPABILITY =
            RuntimeCapability.Builder.of("org.wildfly.threads.executor.messaging-activemq." + CommonAttributes.THREAD_POOL,
                    true,
                    UnboundedQueueThreadPoolResourceDefinition.CAPABILITY.getCapabilityServiceValueType())
                    .setDynamicNameMapper(ThreadPoolCapabilityNameMapper.INSTANCE)
                    .build();

    public static final RuntimeCapability<Void> JOURNAL_THREAD_POOL_CAPABILITY =
            RuntimeCapability.Builder.of("org.wildfly.threads.executor.messaging-activemq." + CommonAttributes.JOURNAL_THREAD_POOL,
                    true,
                    UnboundedQueueThreadPoolResourceDefinition.CAPABILITY.getCapabilityServiceValueType())
                    .setDynamicNameMapper(ThreadPoolCapabilityNameMapper.INSTANCE)
                    .build();


    static final PersistentResourceDefinition JOURNAL_THREAD_POOL = UnboundedQueueThreadPoolResourceDefinition.create(CommonAttributes.JOURNAL_THREAD_POOL,
            ArtemisThreadFactoryResolver.INSTANCE, null, true, JOURNAL_THREAD_POOL_CAPABILITY, false);

    static final PersistentResourceDefinition THREAD_FACTORY = new ThreadFactoryResourceDefinition();

    static final PersistentResourceDefinition SCHEDULED_THREAD_POOL = ScheduledThreadPoolResourceDefinition.create(CommonAttributes.SCHEDULED_THREAD_POOL,
            ArtemisThreadFactoryResolver.INSTANCE, null, true, SCHEDULED_THREAD_POOL_CAPABILITY);

    static final PersistentResourceDefinition THREAD_POOL = UnboundedQueueThreadPoolResourceDefinition.create(CommonAttributes.THREAD_POOL,
            ArtemisThreadFactoryResolver.INSTANCE, null, true, THREAD_POOL_CAPABILITY, false);

    static final PersistentResourceDefinition CLIENT_THREAD_POOL = UnboundedQueueThreadPoolResourceDefinition.create(CommonAttributes.THREAD_POOL,
            ArtemisThreadFactoryResolver.INSTANCE, null, true, THREAD_POOL_CAPABILITY, true);

    static final PersistentResourceDefinition CLIENT_SCHEDULED_THREAD_POOL = ScheduledThreadPoolResourceDefinition.create(CommonAttributes.SCHEDULED_THREAD_POOL,
            ArtemisThreadFactoryResolver.INSTANCE, null, true, SCHEDULED_THREAD_POOL_CAPABILITY);

    private static class ArtemisThreadFactoryResolver extends ThreadFactoryResolver.SimpleResolver {
        static final ArtemisThreadFactoryResolver INSTANCE = new ArtemisThreadFactoryResolver();


        private ArtemisThreadFactoryResolver() {
            super(ServiceNameFactory.parseServiceName("org.wildfly.threads." + CommonAttributes.THREAD_FACTORY));
        }
    }
}
