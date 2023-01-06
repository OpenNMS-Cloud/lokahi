package org.opennms.horizon.inventory.config;

import org.opennms.horizon.shared.azure.http.AzureHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureConfig {

    @Bean
    public AzureHttpClient azureHttpClient() {
        return new AzureHttpClient();
    }
}
