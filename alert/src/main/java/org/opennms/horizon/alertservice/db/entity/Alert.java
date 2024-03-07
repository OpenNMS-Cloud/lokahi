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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.opennms.horizon.alerts.proto.AlertType;
import org.opennms.horizon.alerts.proto.ManagedObjectType;
import org.opennms.horizon.alerts.proto.Severity;

@Entity
@Table(name = "alert")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class Alert implements Serializable {
    @Serial
    private static final long serialVersionUID = 7275548439687562161L;

    @Id
    @SequenceGenerator(name = "alertSequence", sequenceName = "alert_nxt_id", allocationSize = 1)
    @GeneratedValue(generator = "alertSequence")
    @Column(nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(length = 256, nullable = false)
    private String eventUei;

    @Column(unique = true)
    private String reductionKey;

    @Column
    @Enumerated(EnumType.STRING)
    private AlertType type;

    @Column(nullable = false)
    private Long counter;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Severity severity = Severity.INDETERMINATE;

    @Temporal(TemporalType.TIMESTAMP)
    @Column
    private Date firstEventTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column
    private Date lastEventTime;

    @Column
    private String description;

    @Column
    private String logMessage;

    @Column
    private String acknowledgedByUser;

    @Temporal(TemporalType.TIMESTAMP)
    @Column
    private Date acknowledgedAt;

    @Column
    private Long lastEventId;

    @Column
    private String clearKey;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ManagedObjectType managedObjectType;

    @Column
    private String managedObjectInstance;

    @Transient
    private List<Long> monitoringPolicyId;

    @Transient
    private String nodeLabel;

    @OneToOne
    @JoinColumn(name = "alert_condition_id", referencedColumnName = "id")
    private AlertCondition alertCondition;

    public void incrementCount() {
        counter++;
    }

    @Column(name = "node_id")
    private Long nodeId;
}
