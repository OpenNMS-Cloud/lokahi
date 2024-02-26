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
package org.opennms.horizon.inventory.cucumber.steps;

import io.cucumber.java.en.Given;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Assertions;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.dto.MonitoringLocationCreateDTO;

public class CommonStepDefinitions {

    private final InventoryBackgroundHelper backgroundHelper;

    public CommonStepDefinitions(InventoryBackgroundHelper backgroundHelper) {
        this.backgroundHelper = backgroundHelper;
    }

    @Given("[Common] Create {string} Location")
    public void createLocation(String location) {
        var locationServiceBlockingStub = backgroundHelper.getMonitoringLocationStub();
        try {
            var locationDto = locationServiceBlockingStub.createLocation(MonitoringLocationCreateDTO.newBuilder()
                    .setLocation(location)
                    .build());
            Assertions.assertNotNull(locationDto);
        } catch (StatusRuntimeException e) {
            // catch duplicate location
        }
    }
}
