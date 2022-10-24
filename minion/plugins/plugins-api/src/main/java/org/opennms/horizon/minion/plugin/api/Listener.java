package org.opennms.horizon.minion.plugin.api;

/**
 * Interface for a Listener, which is a long-running operation that receives spurious input from external sources, such
 * as a listener for SNMP Traps.
 *
 * NOTE: plugins register the ListenerFactory with the ListenerFactoryRegistry.
 */
public interface Listener {
    void start() throws Exception;
    void stop();
}
