/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.inventory.model;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
public class IpInterface {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    @Column(name = "tenant_id")
    private String tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", referencedColumnName = "id")
    private Node node;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="snmp_interface_id", referencedColumnName = "id")
    private SnmpInterface snmpInterface;

    @Column(name = "snmp_interface_id", insertable = false, updatable = false)
    private Long snmpInterfaceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="azure_interface_id", referencedColumnName = "id")
    private AzureInterface azureInterface;

    @Column(name = "azure_interface_id", insertable = false, updatable = false)
    private Long azureInterfaceId;

    @Column(name = "node_id", insertable = false, updatable = false)
    private long nodeId;

    @NotNull
    @Column(name = "ip_address", columnDefinition = "inet")
    private InetAddress ipAddress;

    @OneToMany(mappedBy = "ipInterface", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<MonitoredService> monitoredServices = new ArrayList<>();

    @Column(name = "snmp_primary")
    private Boolean snmpPrimary;

    @Column(name = "ip_hostname")
    private String hostname;

    @Column(name = "netmask")
    private String netmask;

    @Column(name = "if_index")
    private int ifIndex;
}
