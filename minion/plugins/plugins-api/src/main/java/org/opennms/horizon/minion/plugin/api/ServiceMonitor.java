package org.opennms.horizon.minion.plugin.api;

import com.google.protobuf.Any;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ServiceMonitor {

    /**
     * <P>
     * This method is the heart of the plug-in monitor. Each time an interface
     * requires a check to be performed as defined by the scheduler the poll
     * method is invoked. The poll is passed the service to check.
     * </P>
     *
     * <P>
     * By default when the status transition from up to down or vice versa the
     * framework will generate an event. Additionally, if the polling interval
     * changes due to an extended unavailability, the framework will generate an
     * additional down event. The plug-in can suppress the generation of the
     * default events by setting the suppress event bit in the returned integer.
     * </P>
     *
     * <P>
     * <STRONG>NOTE: </STRONG> This method may be invoked on a Minion, in which
     * case certain bean and facilities will not be available. If any state related
     * information is required such as agent related configuration, it should retrieved
     * by the {@link #getRuntimeAttributes(MonitoredService, Map)}.
     * </P>
     *
     * @param svc
     *            Includes details about to the service being monitored.
     * @param config
     *            Includes the service parameters defined in <EM>poller-configuration.xml</EM> and those
     *            returned by {@link #getRuntimeAttributes(MonitoredService, Map)}.
     * @return The availability of the interface and if a transition event
     *         should be suppressed.
     * @exception RuntimeException
     *                Thrown if an unrecoverable error occurs that prevents the
     *                interface from being monitored.
     * @see PollStatus#SERVICE_AVAILABLE
     * @see PollStatus#SERVICE_UNAVAILABLE
     * @see PollStatus#SERVICE_AVAILABLE
     * @see PollStatus#SERVICE_UNAVAILABLE
     */
    public CompletableFuture<ServiceMonitorResponse> poll(MonitoredService svc, Any config);

    /**
     *
     * @param svc
     *            Includes details about to the service being monitored.
     * @param parameters
     *            Includes the service parameters defined in <EM>poller-configuration.xml</EM> and those
     *            returned by {@link #getRuntimeAttributes(MonitoredService, Map)}.
     * @return Additional attributes, which should be added to the parameter map before calling {@link #poll(MonitoredService, Any)}.
     */
    public Map<String, Object> getRuntimeAttributes(MonitoredService svc, Map<String, Object> parameters);

    /**
     * Allows the monitor to override the location at which it should be run.
     *
     * @param location
     *            location associated with the service to be monitored
     * @return a possibly updated location
     */
    public String getEffectiveLocation(String location);

}
