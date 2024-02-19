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
package org.opennms.horizon.alertservice.db.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.opennms.horizon.alertservice.service.routing.MonitoringPolicyProducer;

@Entity
@EntityListeners(MonitoringPolicyProducer.class)
@Table(name = "monitoring_policy")
@Getter
@Setter
public class MonitorPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "policy_name")
    private String name;

    private String memo;

    @Column(name = "notify_email")
    private Boolean notifyByEmail;

    @Column(name = "notify_pagerduty")
    private Boolean notifyByPagerDuty;

    @Column(name = "notify_webhooks")
    private Boolean notifyByWebhooks;

    @Column(name = "notify_instruction")
    private String notifyInstruction;

    @OneToMany(mappedBy = "policy", orphanRemoval = true, cascade = CascadeType.ALL)
    private List<PolicyRule> rules = new ArrayList<>();

    @ManyToMany(mappedBy = "policies")
    private Set<Tag> tags = new HashSet<>();
}
