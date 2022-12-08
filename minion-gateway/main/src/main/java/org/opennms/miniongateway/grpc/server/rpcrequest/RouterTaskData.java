package org.opennms.miniongateway.grpc.server.rpcrequest;

public class RouterTaskData {
    private final String tenantId;
    private final byte[] requestPayload;

    public RouterTaskData(String tenantId, byte[] requestPayload) {
        this.tenantId = tenantId;
        this.requestPayload = requestPayload;
    }

    public String getTenantId() {
        return tenantId;
    }

    public byte[] getRequestPayload() {
        return requestPayload;
    }
}
