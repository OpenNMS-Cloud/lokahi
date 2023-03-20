package org.opennms.horizon.minion.taskset.worker.impl;

import java.util.Map;
import org.opennms.horizon.minion.plugin.api.MonitoredService;

import java.net.InetAddress;
import org.opennms.taskset.contract.TaskContext;

// TODO: why an interface - this seems like a straight-forward model?
public class GeneralMonitoredService implements MonitoredService {

    private final String svcName;
    private final String ipAddr;
    private final TaskContext taskContext;
    private final String nodeLabel;
    private final String nodeLocation;
    private final InetAddress address;

    public GeneralMonitoredService(String svcName, String ipAddr, TaskContext taskContext, String nodeLabel, String nodeLocation, InetAddress address) {
        this.svcName = svcName;
        this.ipAddr = ipAddr;
        this.taskContext = taskContext;
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
    public long getNodeId() {
        return taskContext.getNodeId();
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
