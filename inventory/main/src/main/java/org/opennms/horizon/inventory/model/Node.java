/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.opennms.horizon.inventory.component.NodeKafkaProducer;
import org.opennms.horizon.inventory.dto.MonitoredState;
import org.opennms.taskset.contract.ScanType;

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

    @Column(name = "active_discovery_ids", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Long> discoveryIds = new ArrayList<>();

    @NotNull
    @Column(name = "node_label")
    private String nodeLabel;

    @Column(name = "node_alias")
    private String nodeAlias;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "scan_type")
    private ScanType scanType;

    @Enumerated(EnumType.STRING)
    @Column(name = "monitored_state")
    private MonitoredState monitoredState = MonitoredState.DETECTED;

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

    @OneToMany(mappedBy = "node", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<AzureInterface> azureInterfaces = new ArrayList<>();

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
