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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

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
    @JoinColumn(name = "snmp_interface_id", referencedColumnName = "id")
    private SnmpInterface snmpInterface;

    @Column(name = "snmp_interface_id", insertable = false, updatable = false)
    private Long snmpInterfaceId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "azure_interface_id", referencedColumnName = "id")
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
