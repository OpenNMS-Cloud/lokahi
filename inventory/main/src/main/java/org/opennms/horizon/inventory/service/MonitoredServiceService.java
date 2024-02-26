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
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.inventory.dto.MonitoredServiceDTO;
import org.opennms.horizon.inventory.mapper.MonitoredServiceMapper;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.MonitoredService;
import org.opennms.horizon.inventory.model.MonitoredServiceType;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.inventory.repository.MonitoredServiceRepository;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MonitoredServiceService {

    private final MonitoredServiceRepository modelRepo;
    private final MonitoredServiceMapper mapper;
    private final IpInterfaceRepository ipInterfaceRepository;

    public MonitoredService createSingle(
            MonitoredServiceDTO newMonitoredService,
            MonitoredServiceType monitoredServiceType,
            IpInterface ipInterface) {

        String tenantId = newMonitoredService.getTenantId();

        Optional<MonitoredService> monitoredServiceOpt =
                modelRepo.findByTenantIdTypeAndIpInterface(tenantId, monitoredServiceType, ipInterface);

        if (monitoredServiceOpt.isEmpty()) {

            MonitoredService monitoredService = mapper.dtoToModel(newMonitoredService);
            monitoredService.setIpInterface(ipInterface);
            monitoredService.setMonitoredServiceType(monitoredServiceType);

            modelRepo.save(monitoredService);
            return monitoredService;
        }
        return monitoredServiceOpt.get();
    }

    public List<MonitoredServiceDTO> findByTenantId(String tenantId) {
        List<MonitoredService> all = modelRepo.findByTenantId(tenantId);
        return all.stream().map(mapper::modelToDTO).collect(Collectors.toList());
    }

    public Optional<MonitoredServiceDTO> findMonitoredService(
            String tenantId, String ipAddress, String monitorType, long nodeId) {

        var optionalIpInterface = ipInterfaceRepository.findByNodeIdAndTenantIdAndIpAddress(
                nodeId, tenantId, InetAddressUtils.addr(ipAddress));
        if (optionalIpInterface.isPresent()) {
            var optionalService = modelRepo.findByServiceNameAndIpInterfaceId(
                    tenantId, monitorType, optionalIpInterface.get().getId());
            return optionalService.map(mapper::modelToDTO);
        }
        return Optional.empty();
    }
}
