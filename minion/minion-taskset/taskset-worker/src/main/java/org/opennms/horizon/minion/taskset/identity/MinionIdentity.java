package org.opennms.horizon.minion.taskset.identity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;

@Getter
@AllArgsConstructor
public class MinionIdentity implements IpcIdentity {

    private final String id;
    private final String location;
}
