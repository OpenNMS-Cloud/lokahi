package org.opennms.horizon.it.gqlmodels;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetricIdentityData {
    private String __name__;
    private String instance;
    private String location;
    private String monitor;

    @JsonProperty("node_id")
    private String nodeId;

    @JsonProperty("pushgateway_instance")
    private String pushgatewayInstance;

    @JsonProperty("system_id")
    private String systemId;
}
