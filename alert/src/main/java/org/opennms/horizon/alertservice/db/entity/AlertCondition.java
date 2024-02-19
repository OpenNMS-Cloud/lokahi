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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.opennms.horizon.alerts.proto.OverTimeUnit;
import org.opennms.horizon.alerts.proto.Severity;

@Entity
@Table(name = "alert_condition")
@Getter
@Setter
public class AlertCondition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @ManyToOne
    @JoinColumn(name = "trigger_event_definition_id", referencedColumnName = "id")
    private EventDefinition triggerEvent;

    @Column(name = "event_count")
    private Integer count;

    @Column(name = "over_time")
    private Integer overtime;

    @Enumerated(EnumType.STRING)
    @Column(name = "over_time_unit")
    private OverTimeUnit overtimeUnit;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @ManyToOne
    @JoinColumn(name = "clear_event_definition_id", referencedColumnName = "id")
    private EventDefinition clearEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_id", referencedColumnName = "id")
    private PolicyRule rule;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "alertCondition")
    private AlertDefinition alertDefinition;
}
