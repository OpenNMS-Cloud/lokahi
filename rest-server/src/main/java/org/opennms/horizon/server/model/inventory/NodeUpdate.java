package org.opennms.horizon.server.model.inventory;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NodeUpdate {
    private long id;
    private String nodeAlias;
    private List<Long> discoveryIds;
}
