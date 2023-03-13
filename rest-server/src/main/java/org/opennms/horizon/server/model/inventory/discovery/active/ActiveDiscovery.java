package org.opennms.horizon.server.model.inventory.discovery.active;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActiveDiscovery {
    private String discoveryType;
    private JsonNode details;
}
