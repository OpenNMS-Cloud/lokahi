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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.NotNull;
import java.net.InetAddress;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
public class SnmpInterface extends TenantAwareEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name="node_id", referencedColumnName = "id"),
        @JoinColumn(name="node_tenant_id", referencedColumnName = "tenant_id")
    })
    private Node node;

    @Column(name = "node_id", insertable = false, updatable = false)
    private long nodeId;

    @Column(name = "node_tenant_id", insertable = false, updatable = false)
    private long nodeTenantId;

    @Column(name = "ip_address", columnDefinition = "inet")
    private InetAddress ipAddress;

    @NotNull
    @Column(name = "if_index")
    private int ifIndex;

    @Column(name = "if_descr")
    private String ifDescr;

    @Column(name = "if_type")
    private int ifType;

    @Column(name = "if_name")
    private String ifName;

    @Column(name = "if_speed")
    private long ifSpeed;

    @Column(name = "if_admin_status")
    private int ifAdminStatus;

    @Column(name = "if_operator_status")
    private int ifOperatorStatus;

    @Column(name = "if_alias")
    private String ifAlias;

    @Column(name = "physical_address")
    private String physicalAddr;
}
