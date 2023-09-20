package org.opennms.horizon.minion.grpc;

public interface ReconnectStrategy {

    void activate();

    void shutdown();

}
