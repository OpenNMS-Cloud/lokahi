package org.opennms.horizon.minion.grpc;

import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;

public class MinionIpcIdentity implements IpcIdentity {

    private final String systemId;
    private final String location;

    public MinionIpcIdentity(String systemId, String location) {
        this.systemId = systemId;
        this.location = location;
    }

    @Override
    public String getId() {
        return systemId;
    }

    @Override
    public String getLocation() {
        return location;
    }

}
