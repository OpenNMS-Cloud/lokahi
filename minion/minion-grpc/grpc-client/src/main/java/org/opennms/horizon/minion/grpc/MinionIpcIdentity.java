package org.opennms.horizon.minion.grpc;

import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;

public class MinionIpcIdentity implements IpcIdentity {

    private final String systemId;

    public MinionIpcIdentity(String systemId) {
        this.systemId = systemId;
    }

    @Override
    public String getId() {
        return systemId;
    }

}
