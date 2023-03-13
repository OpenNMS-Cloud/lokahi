package org.opennms.horizon.inventory.model.discovery.active;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Getter
@Setter
@Entity(name = "icmp_active_discovery")
public class IcmpActiveDiscovery extends ActiveDiscovery {

    @NotNull
    @Column(name = "ip_address_entries", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> ipAddressEntries;

    @NotNull
    @Column(name = "snmp_community_strings", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> snmpCommunityStrings;

    @NotNull
    @Column(name = "snmp_ports", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Integer> snmpPorts;
}
