package org.opennms.horizon.minion.taskset.worker.impl;

import org.opennms.horizon.minion.plugin.api.MonitoredService;

import java.net.InetAddress;

// TODO: why an interface - this seems like a straight-forward model?
public class GeneralMonitoredService implements MonitoredService {

    private final String svcName;
    private final String ipAddr;
    private final long nodeId;
    private final String nodeLabel;
    private final String nodeLocation;
    private final InetAddress address;
    private final long monitorServiceId;

    public GeneralMonitoredService(String svcName, String ipAddr, long nodeId, String nodeLabel,
                                   String nodeLocation, InetAddress address, long monitorServiceId) {
        this.svcName = svcName;
        this.ipAddr = ipAddr;
        this.nodeId = nodeId;
        this.nodeLabel = nodeLabel;
        this.nodeLocation = nodeLocation;
        this.address = address;
        this.monitorServiceId = monitorServiceId;
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
    public long getNodeId() {
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

    @Override
    public long getMonitorServiceId() {
        return monitorServiceId;
    }
}
