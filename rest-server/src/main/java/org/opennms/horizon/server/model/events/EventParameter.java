package org.opennms.horizon.server.model.events;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EventParameter {
    private String name;
    private String value;
    private String type;
    private String encoding;
}
