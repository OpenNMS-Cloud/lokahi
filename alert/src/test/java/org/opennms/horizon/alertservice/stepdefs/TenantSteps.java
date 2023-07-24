/*
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
 */

package org.opennms.horizon.alertservice.stepdefs;

import io.cucumber.java.en.Given;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.alertservice.AlertGrpcClientUtils;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class TenantSteps {
    private final AlertGrpcClientUtils grpcClient;

    @Getter
    private String tenantId;

    /**
     * Uses a new random tenantId.
     */
    @Given("A new tenant")
    public String useRandomizedTenantId() {
        return useSpecifiedTenantId(String.format("tenant-%s", UUID.randomUUID()));
    }

    /**
     * Uses the specified tenantId. Note that if a feature specifies a
     * hard-coded tenant in a Background section, then those setup steps are
     * applied to the same tenant <i>on every scenario</i>, leading to duplicate
     * monitoring policies, etc.
     */
    @Given("Tenant id {string}")
    @Given("Tenant {string}")
    @Given("A new tenant named {string}")
    public String useSpecifiedTenantId(String tenantId) {
        this.tenantId = tenantId;
        grpcClient.setTenantId(tenantId);
        log.info("New tenant-id is {}", tenantId);
        return tenantId;
    }
}
