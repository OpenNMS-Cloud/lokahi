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
package org.opennms.horizon.inventory.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.SnmpInterfaceDTO;
import org.opennms.horizon.inventory.mapper.SnmpInterfaceMapper;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.SnmpInterface;
import org.opennms.horizon.inventory.repository.SnmpInterfaceRepository;
import org.opennms.node.scan.contract.SnmpInterfaceResult;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SnmpInterfaceService {
    private final SnmpInterfaceRepository modelRepo;

    private final SnmpInterfaceMapper mapper;

    public List<SnmpInterfaceDTO> findByTenantId(String tenantId) {
        List<SnmpInterface> all = modelRepo.findByTenantId(tenantId);
        return all.stream().map(mapper::modelToDTO).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public SnmpInterface createOrUpdateFromScanResult(String tenantId, Node node, SnmpInterfaceResult result) {
        return modelRepo
                .findByNodeIdAndTenantIdAndIfIndex(node.getId(), tenantId, result.getIfIndex())
                .map(snmp -> {
                    mapper.updateFromScanResult(result, snmp);
                    modelRepo.save(snmp);
                    return snmp;
                })
                .orElseGet(() -> {
                    SnmpInterface snmp = mapper.scanResultToModel(result);
                    snmp.setNode(node);
                    snmp.setTenantId(tenantId);
                    return modelRepo.save(snmp);
                });
    }
}
