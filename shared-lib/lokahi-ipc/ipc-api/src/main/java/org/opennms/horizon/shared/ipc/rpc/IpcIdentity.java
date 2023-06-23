package org.opennms.horizon.shared.ipc.rpc;

/**
 * @deprecated Please migrate to project/module specific API. The Ipcidentity interface is not going to be consistent
 * across minion and backend.
 */
@Deprecated
public interface IpcIdentity {
    String getId();
}
