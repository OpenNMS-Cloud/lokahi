package org.opennms.horizon.minion.taskset.worker.impl;

import java.net.InetAddress;
import org.opennms.horizon.minion.plugin.api.MonitoredService;

// TODO: why an interface - this seems like a straight-forward model?
public class GeneralMonitoredService implements MonitoredService {

    private final String svcName;
    private final String ipAddr;
    private final int nodeId;
    private final String nodeLabel;
    private final String nodeLocation;
    private final InetAddress address;

    public GeneralMonitoredService(String svcName, String ipAddr, int nodeId, String nodeLabel, String nodeLocation, InetAddress address) {
        this.svcName = svcName;
        this.ipAddr = ipAddr;
        this.nodeId = nodeId;
        this.nodeLabel = nodeLabel;
        this.nodeLocation = nodeLocation;
        this.address = address;
    }

    @Override
    public String getSvcName() {
        return svcName;
    }

    @Override
    public String getIpAddr() {
        return ipAddr;
    }

    @Override
    public int getNodeId() {
        return nodeId;
    }

    @Override
    public String getNodeLabel() {
        return nodeLabel;
    }

    @Override
    public String getNodeLocation() {
        return nodeLocation;
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }
}
