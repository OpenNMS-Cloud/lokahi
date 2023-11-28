package org.opennms.horizon.server.model.inventory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NodeUpdate {
    private long id;
    private String nodeAlias;
}
