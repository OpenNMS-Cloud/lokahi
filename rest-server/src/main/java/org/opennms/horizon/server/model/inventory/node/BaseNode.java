package org.opennms.horizon.server.model.inventory.node;

import lombok.Getter;
import lombok.Setter;
import org.opennms.horizon.server.model.inventory.IpInterface;

import java.util.List;

@Getter
@Setter
public class BaseNode {
    private long id;
    private String tenantId;
    private String nodeLabel;
    private String scanType;
    private String monitoredState;
    private long createTime;
    private long monitoringLocationId;
    private String monitoringLocation;
    private List<IpInterface> ipInterfaces;
}
