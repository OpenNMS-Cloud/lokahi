package org.opennms.horizon.server.model.inventory;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SnmpInterfaceCreate {

    private String tenantId ;
    private String nodeLabel;
    private int  ifIndex ;
    private String ifDescr ;
    private int  ifType ;
    private String ifName ;
    private long ifSpeed ;
    private int ifAdminStatus ;
    private int ifOperatorStatus ;
    private String ifAlias ;
    private String physicalAddr ;
}
