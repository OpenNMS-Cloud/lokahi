package org.opennms.horizon.shared.ipc.grpc.server.manager;

import java.util.List;

public interface MinionManager {
    void addMinion(MinionInfo minionInfo);
    void removeMinion(MinionInfo minionInfo);
    void addMinionListener(MinionManagerListener listener);
    void removeMinionListener(MinionManagerListener listener);
    List<MinionInfo> getMinions();
}
