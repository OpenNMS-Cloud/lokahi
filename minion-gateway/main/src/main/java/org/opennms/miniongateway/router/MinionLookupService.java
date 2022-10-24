package org.opennms.miniongateway.router;

import java.util.List;
import java.util.UUID;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionManagerListener;

public interface MinionLookupService extends MinionManagerListener {

    String IGNITE_SERVICE_NAME = "minionLookup";

    UUID findGatewayNodeWithId(String id);

    /**
     * Returns list of minions assigned to given location.
     *
     * @param location Location name.
     * @return Minions assigned to location or null if none found.
     */
    List<UUID> findGatewayNodeWithLocation(String location);
}
