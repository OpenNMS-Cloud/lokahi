package org.opennms.horizon.server.model.events;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SnmpInfo {
    private String id;
    private String version;
    private int specific;
    private int generic;
    private String community;
    private String trapOid;
}
