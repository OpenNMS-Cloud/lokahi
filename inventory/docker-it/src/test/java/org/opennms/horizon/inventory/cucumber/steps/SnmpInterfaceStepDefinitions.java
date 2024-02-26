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
 *//*

package org.opennms.horizon.inventory.cucumber.steps;

import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cucumber.java.en.Then;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.inventory.cucumber.InventoryBackgroundHelper;
import org.opennms.horizon.inventory.dto.SearchBy;
import org.opennms.horizon.inventory.dto.SnmpInterfaceDTO;
import org.opennms.horizon.inventory.dto.SnmpInterfacesList;

@Slf4j
public class SnmpInterfaceStepDefinitions {
    private final InventoryBackgroundHelper backgroundHelper;

    public SnmpInterfaceStepDefinitions(InventoryBackgroundHelper backgroundHelper) {
        this.backgroundHelper = backgroundHelper;
    }

 */
/*   @Then("fetch a list of snmp_interface by name with search {string}")
    public void fetchAListOfSnmpInterfaceByNameWithSearch(String search) {
        var nodeServiceBlockingStub = backgroundHelper.getNodeServiceBlockingStub();
        SnmpInterfacesList list = nodeServiceBlockingStub.listSnmpInterfaces(
                SearchBy.newBuilder().setSearch(search).build());
        list.getSnmpInterfacesList().stream()
                .map(SnmpInterfaceDTO::getIfName)
                .forEach(label -> assertTrue(label.contains(search)));
    }*//*

}
*/
