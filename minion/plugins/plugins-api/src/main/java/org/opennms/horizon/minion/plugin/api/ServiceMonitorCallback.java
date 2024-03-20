package org.opennms.horizon.minion.plugin.api;

public interface ServiceMonitorCallback {
    public void onResponse(ServiceMonitorResponse response);
}
