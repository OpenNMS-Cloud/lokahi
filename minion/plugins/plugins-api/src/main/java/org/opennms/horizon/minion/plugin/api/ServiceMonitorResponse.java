package org.opennms.horizon.minion.plugin.api;

import org.opennms.taskset.contract.MonitorType;

import java.util.Map;

public interface ServiceMonitorResponse {
    /**
     *
     * @return type of monitor that produced the response.
     */
    MonitorType getMonitorType();

    /**
     *
     * @return status whether service is Unknown/Up/Down/Unresponsive
     */
    Status getStatus();

    /**
     *
     * @return reason behind the current poll status when the service is not Up
     */
    String getReason();

    /**
     *
     * @return IP address that was monitored
     */
    String getIpAddress();


    long getNodeId();

    long getMonitoredServiceId();

    /**
     *  TODO: standardize the unit (ms or sec?)
     *
     * @return amount of time device took to respond to the monitor request
     */
    double getResponseTime();

    Map<String, Number> getProperties();

    /**
     * TECHDEBT: Was originally added to the monitor interface to take advantage of poller's scheduling and
     * configuration mechanism.
     */
    DeviceConfig getDeviceConfig();

    /**
     * Returns timestamp when response time was actually generated.
     *
     * @return Timestamp of a response.
     */
    long getTimestamp();

    interface DeviceConfig {

        byte[] getContent();

        String getFilename();

    }

    enum Status {
        /**
         * Was unable to determine the status.
         */
        Unknown,

        /**
         * Was in a normal state.
         */
        Up,

        /**
         * Not working normally.
         */
        Down,

        /**
         * Service that is up but is most likely suffering due to excessive load or latency
         * issues and because of that has not responded within the configured timeout period.
         */
        Unresponsive;
    }
}
