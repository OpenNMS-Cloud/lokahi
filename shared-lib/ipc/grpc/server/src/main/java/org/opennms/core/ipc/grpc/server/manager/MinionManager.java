package org.opennms.core.ipc.grpc.server.manager;

import java.util.List;

public interface MinionManager {
    void addMinion(MinionInfo minionInfo);
    void removeMinion(String minionId);
    void addMinionListener(MinionManagerListener listener);
    void removeMinionListener(MinionManagerListener listener);
    List<MinionInfo> getMinions();
}
