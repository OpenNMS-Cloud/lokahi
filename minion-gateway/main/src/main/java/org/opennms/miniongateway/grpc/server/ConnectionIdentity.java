package org.opennms.miniongateway.grpc.server;

import java.util.Objects;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;

public class ConnectionIdentity implements IpcIdentity {

    private final Identity identity;

    public ConnectionIdentity(Identity identity) {
        this.identity = identity;
    }

    @Override
    public String getId() {
        return identity.getSystemId();
    }

    @Override
    public String getLocation() {
        return identity.getLocation();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConnectionIdentity)) {
            return false;
        }
        ConnectionIdentity that = (ConnectionIdentity) o;
        return Objects.equals(identity, that.identity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identity);
    }

}
