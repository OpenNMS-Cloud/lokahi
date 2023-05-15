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

package org.opennms.horizon.alertservice.db.entity;

import java.util.ArrayList;
import java.util.List;

import org.opennms.horizon.alertservice.service.routing.MonitoringPolicyProducer;

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
import lombok.Getter;
import lombok.Setter;

@Entity
@EntityListeners(MonitoringPolicyProducer.class)
@Table(name = "monitoring_policy")
@Getter
@Setter
public class MonitorPolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column (name = "tenant_id", nullable = false)
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
    private List<Tag> tags = new ArrayList<>();
}
