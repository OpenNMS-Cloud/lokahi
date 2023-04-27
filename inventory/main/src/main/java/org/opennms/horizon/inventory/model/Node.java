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
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.opennms.horizon.inventory.component.NodeKafkaProducer;
import org.opennms.horizon.inventory.dto.MonitoredState;
import org.opennms.taskset.contract.ScanType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
@EntityListeners(NodeKafkaProducer.class)
public class Node {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    @Column(name = "tenant_id")
    private String tenantId;

    @NotNull
    @Column(name = "node_label")
    private String nodeLabel;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "scan_type")
    private ScanType scanType;

    @Enumerated(EnumType.STRING)
    @Column(name = "monitored_state")
    private MonitoredState monitoredState;

    @NotNull
    @Column(name = "create_time", columnDefinition = "TIMESTAMP")
    private LocalDateTime createTime;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "monitoring_location_id", referencedColumnName = "id")
    private MonitoringLocation monitoringLocation;

    @Column(name = "monitoring_location_id", insertable = false, updatable = false)
    private long monitoringLocationId;

    @OneToMany(mappedBy = "node", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<IpInterface> ipInterfaces = new ArrayList<>();

    @OneToMany(mappedBy = "node", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<SnmpInterface> snmpInterfaces = new ArrayList<>();

    @ManyToMany(mappedBy = "nodes")
    private List<Tag> tags = new ArrayList<>();

    @Column(name = "system_objectid")
    private String objectId;
    @Column(name = "system_name")
    private String systemName;
    @Column(name = "system_desc")
    private String systemDescr;
    @Column(name = "system_location")
    private String systemLocation;
    @Column(name = "system_contact")
    private String systemContact;
}
