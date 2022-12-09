package org.opennms.horizon.server.model.status;

import lombok.Getter;

@Getter
public class NodeStatus {
    private final long id;
    private final String status;

    public NodeStatus(long id, boolean status) {
        this.id = id;
        this.status = status ? "UP" : "DOWN";
    }
}
