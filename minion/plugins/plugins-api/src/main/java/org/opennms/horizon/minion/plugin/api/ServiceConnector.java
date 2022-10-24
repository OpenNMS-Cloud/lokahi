package org.opennms.horizon.minion.plugin.api;

/**
 * Interface for a ServiceConnector, which is a long-running operation that creates a long-lived connection to a remote
 * endpoint and consumes a stream of samples.
 *
 * NOTE: plugins register the ServiceConnectorFactory with the ServiceConnectorFactoryRegistry.
 */
public interface ServiceConnector {
    /**
     * Attempt to create a connection to the remote.  Implementations may use their own disconnect/reconnect logic,
     * but it's not required - the Minion will automatically attempt to reconnect after failed connections / disconnects.
     *
     * @throws Exception
     */
    void connect() throws Exception;

    /**
     * Shutdown the active connection.
     */
    void disconnect();
}
