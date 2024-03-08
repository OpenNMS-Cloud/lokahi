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
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;
import org.opennms.horizon.alerts.proto.AlertType;
import org.opennms.horizon.alerts.proto.ManagedObjectType;

@Entity
@Table(name = "alert_definition")
@Getter
@Setter
public class AlertDefinition implements Serializable {

    @Serial
    private static final long serialVersionUID = 7825119801706458618L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(length = 256, nullable = false)
    private String uei;

    @Column(name = "reduction_key", nullable = false)
    private String reductionKey;

    @Column(name = "clear_key")
    private String clearKey;

    @Column
    @Enumerated(EnumType.STRING)
    private AlertType type;

    @Column(name = "managed_object_type")
    @Enumerated(EnumType.STRING)
    private ManagedObjectType managedObjectType;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_condition_id", referencedColumnName = "id")
    private AlertCondition alertCondition;
}
