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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "thresholded_event")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class ThresholdedEvent implements Serializable {
    @Serial
    private static final long serialVersionUID = 7275548439687562161L;

    @Id
    @SequenceGenerator(name = "thresholdedEventSequence", sequenceName = "thresholded_event_nxt_id", allocationSize = 1)
    @GeneratedValue(generator = "thresholdedEventSequence")
    @Column(nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(length = 256, nullable = false)
    private String eventUei;

    @Column(unique = true)
    private String reductionKey;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "create_time")
    private Date createTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "expiry_time")
    private Date expiryTime;
}
