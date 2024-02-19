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
package org.opennms.horizon.server.mapper.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.ActiveDiscoveryDTO;
import org.opennms.horizon.server.model.inventory.discovery.active.ActiveDiscovery;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActiveDiscoveryMapper {
    private static final String AZURE_DISCOVERY_TYPE = "AZURE";
    private static final String ICMP_DISCOVERY_TYPE = "ICMP";
    private final IcmpActiveDiscoveryMapper icmpMapper;
    private final AzureActiveDiscoveryMapper azureMapper;
    private final ObjectMapper objectMapper;

    public ActiveDiscovery dtoToActiveDiscovery(ActiveDiscoveryDTO activeDiscoveryDTO) {
        ActiveDiscovery discovery = new ActiveDiscovery();
        if (activeDiscoveryDTO.hasAzure()) {
            discovery.setDetails(
                    objectMapper.valueToTree(azureMapper.dtoToAzureActiveDiscovery(activeDiscoveryDTO.getAzure())));
            discovery.setDiscoveryType(AZURE_DISCOVERY_TYPE);
        } else if (activeDiscoveryDTO.hasIcmp()) {
            discovery.setDetails(
                    objectMapper.valueToTree(icmpMapper.dtoToIcmpActiveDiscovery(activeDiscoveryDTO.getIcmp())));
            discovery.setDiscoveryType(ICMP_DISCOVERY_TYPE);
        } else {
            throw new RuntimeException("Invalid Active Discovery type returned");
        }
        return discovery;
    }
}
