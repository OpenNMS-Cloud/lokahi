package org.opennms.miniongateway.grpc.server;

import java.util.Objects;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;

public class ConnectionIdentity implements IpcIdentity {

    private final String systemId;

    public ConnectionIdentity(Identity identity) {
        this(identity.getSystemId());
    }

    public ConnectionIdentity(String systemId) {
        this.systemId = systemId;
    }

    @Override
    public String getId() {
        return systemId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConnectionIdentity that)) {
            return false;
        }
        return Objects.equals(systemId, that.systemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(systemId);
    }
}
