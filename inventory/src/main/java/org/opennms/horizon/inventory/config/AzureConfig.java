package org.opennms.horizon.inventory.config;

import org.opennms.horizon.shared.azure.http.AzureHttpClient;
import org.opennms.horizon.shared.azure.http.dto.AzureHttpParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureConfig {

    @Bean
    public AzureHttpParams azureHttpParams(@Value("${inventory.azure.login-url}") String loginUrl,
                                           @Value("${inventory.azure.management-url}") String managementUrl,
                                           @Value("${inventory.azure.api-version}") String apiVersion,
                                           @Value("${inventory.azure.metrics-api-version}") String metricsApiVersion) {

        AzureHttpParams params = new AzureHttpParams();
        params.setBaseLoginUrl(loginUrl);
        params.setBaseManagementUrl(managementUrl);
        params.setApiVersion(apiVersion);
        params.setApiVersion(metricsApiVersion);
        return params;
    }

    @Bean
    public AzureHttpClient azureHttpClient(AzureHttpParams params) {
        return new AzureHttpClient(params);
    }
}
