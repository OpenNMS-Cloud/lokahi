package org.opennms.horizon.testtool.miniongateway.wiremock.api;

public interface MockTwinHandler {
    void publish(String topic, byte[] content);
}
