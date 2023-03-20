/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
public class SyntheticTest extends TenantAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "synthetic_transaction_id", referencedColumnName = "id")
    private SyntheticTransaction syntheticTransaction;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "synthetic_test_location",
        joinColumns = {@JoinColumn(name = "synthetic_test_id")},
        inverseJoinColumns = @JoinColumn(name = "monitoring_location_id")
    )
    private List<MonitoringLocation> locations;

    @NotNull
    @Column(name = "label")
    private String label;

    @NotNull
    @Column(name = "plugin_name")
    private String pluginName;

    @Column(name = "schedule")
    private String schedule;

    @OneToMany(mappedBy = "syntheticTest", orphanRemoval = true, cascade = CascadeType.ALL)
    @MapKeyColumn(name = "parameter")
    private Map<String, SyntheticTestConfig> config;

    @Column(name = "timeout")
    private long timeout;

    @Column(name = "retries")
    private int retries;
}
