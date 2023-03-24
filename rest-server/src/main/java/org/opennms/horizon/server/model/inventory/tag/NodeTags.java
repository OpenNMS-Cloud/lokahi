package org.opennms.horizon.server.model.inventory.tag;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class NodeTags {
    private long nodeId;
    private List<Tag> tags;
}
