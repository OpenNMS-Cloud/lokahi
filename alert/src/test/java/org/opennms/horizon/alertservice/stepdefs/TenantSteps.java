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
package org.opennms.horizon.alertservice.stepdefs;

import io.cucumber.java.en.Given;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.alertservice.AlertGrpcClientUtils;

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
