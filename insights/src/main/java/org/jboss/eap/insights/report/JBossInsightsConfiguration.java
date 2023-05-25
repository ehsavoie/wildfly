/* Copyright (C) Red Hat 2023 */
package org.jboss.eap.insights.report;

import com.redhat.insights.config.DefaultInsightsConfiguration;
import com.redhat.insights.config.EnvAndSysPropsInsightsConfiguration;

/**
 * Configuration where values from {@link DefaultInsightsConfiguration} can be
 * overridden by environment variables and system properties.
 *
 * <p>
 * Environment variables take priority over system properties.
 */
class JBossInsightsConfiguration extends EnvAndSysPropsInsightsConfiguration {

    /**
     * For testing purpose only
     */
    public static final String ENV_UNSUPPORTED_CONFIGURATION_TRUST_ALL = "UNSUPPORTED_CONFIGURATION_TRUST_ALL";
    /**
     * For testing purpose only
     */
    public static final String ENV_UNSUPPORTED_MACHINE_ID_FILE_PATH = "UNSUPPORTED_MACHINE_ID_FILE_PATH";

    @Override
    public String getMachineIdFilePath() {
        String value = lookup(ENV_UNSUPPORTED_MACHINE_ID_FILE_PATH);
        if (value != null) {
            return value;
        }
        return super.getMachineIdFilePath();
    }

    public boolean isTrustAll() {
        return Boolean.parseBoolean(lookup(ENV_UNSUPPORTED_CONFIGURATION_TRUST_ALL));
    }

    private String lookup(String env) {
        String value = System.getenv(env);
        if (value == null) {
            value = System.getProperty(env.toLowerCase().replace('_', '.'));
        }
        return value;
    }

    @Override
    public String getCertHelperBinary() {
        String value = lookup(ENV_CERT_HELPER_BINARY);
        if (value != null) {
            return value;
        }
        return "/opt/rh/eap7/root/opt/jboss-cert-helper";
    }
}
