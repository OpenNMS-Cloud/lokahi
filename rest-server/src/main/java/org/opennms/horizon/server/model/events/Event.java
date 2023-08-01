package org.opennms.horizon.server.model.events;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Event {
    private int id;
    private String uei;
    private int nodeId;
    private String ipAddress;
    private long producedTime;
    private List<EventParameter> eventParams;
    private EventInfo eventInfo;
}
