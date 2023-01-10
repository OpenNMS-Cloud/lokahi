package org.opennms.horizon.shared.azure.http.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AzureHttpParams {
    private String baseManagementUrl;
    private String baseLoginUrl;
    private String apiVersion;
    private String metricsApiVersion;
}
