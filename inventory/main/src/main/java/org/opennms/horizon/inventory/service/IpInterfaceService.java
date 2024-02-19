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

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.azure.api.AzureScanNetworkInterfaceItem;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.mapper.IpInterfaceMapper;
import org.opennms.horizon.inventory.model.AzureInterface;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.SnmpInterface;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.node.scan.contract.IpInterfaceResult;
import org.springframework.stereotype.Service;

/**
 * TODO: perhaps rename this to ScanResultIpProcessor, or the like.  The name IpInterfaceService can easily lead to
 * tight coupling problems because "ip interface" is a lower-level logical concept than "Azure Scan" and "SNMP".
 * Alternatively, move the azure-specific and snmp-specific handling up a layer and make the operations here work
 * on ip-interface (and lower) concepts only.
 */
@Service
@RequiredArgsConstructor
public class IpInterfaceService {
    private final IpInterfaceRepository modelRepo;

    private final IpInterfaceMapper mapper;

    public List<IpInterfaceDTO> findByTenantId(String tenantId) {
        List<IpInterface> all = modelRepo.findByTenantId(tenantId);
        return all.stream().map(mapper::modelToDTO).collect(Collectors.toList());
    }

    public Optional<IpInterfaceDTO> getByIdAndTenantId(long id, String tenantId) {
        return modelRepo.findByIdAndTenantId(id, tenantId).map(mapper::modelToDTO);
    }

    public Optional<IpInterfaceDTO> findByIpAddressAndLocationIdAndTenantId(
            String ipAddress, String location, String tenantId) {
        return findByIpAddressAndLocationIdAndTenantId(
                InetAddressUtils.getInetAddress(ipAddress), Long.valueOf(location), tenantId);
    }

    public Optional<IpInterfaceDTO> findByIpAddressAndLocationIdAndTenantId(
            InetAddress ipAddress, long locationId, String tenantId) {
        return findByIpAddressAndLocationIdAndTenantIdModel(ipAddress, locationId, tenantId)
                .map(mapper::modelToDTO);
    }

    /**
     * return single IpInterface
     * if multiple found, try return snmp primary one or first one
     * @param ipAddress
     * @param locationId
     * @param tenantId
     * @return
     */
    public Optional<IpInterface> findByIpAddressAndLocationIdAndTenantIdModel(
            InetAddress ipAddress, long locationId, String tenantId) {
        List<IpInterface> ipInterfaces =
                modelRepo.findByIpAddressAndLocationIdAndTenantId(ipAddress, locationId, tenantId);
        if (ipInterfaces.isEmpty()) {
            return Optional.empty();
        } else if (ipInterfaces.size() == 1) {
            return Optional.of(ipInterfaces.get(0));
        } else {
            var result = ipInterfaces.stream()
                    .filter(ipInterface -> ipInterface.getSnmpPrimary() != null && ipInterface.getSnmpPrimary())
                    .findFirst();
            if (result.isPresent()) {
                return result;
            } else {
                return Optional.of(ipInterfaces.get(0));
            }
        }
    }

    public void createFromAzureScanResult(
            String tenantId,
            Node node,
            AzureInterface azureInterface,
            AzureScanNetworkInterfaceItem networkInterfaceItem) {
        Objects.requireNonNull(azureInterface);

        IpInterface ipInterface = new IpInterface();
        ipInterface.setNode(node);
        ipInterface.setTenantId(tenantId);
        ipInterface.setSnmpPrimary(networkInterfaceItem.getIsPrimary());
        ipInterface.setIpAddress(InetAddressUtils.getInetAddress(networkInterfaceItem.getIpAddress()));
        ipInterface.setAzureInterface(azureInterface);
        modelRepo.save(ipInterface);
    }

    public IpInterface getPrimaryInterfaceForNode(long nodeId) {
        var optionalIpInterface = modelRepo.findByNodeIdAndSnmpPrimary(nodeId, true);
        return optionalIpInterface.orElseThrow();
    }

    // TODO: is this executed inside a transaction?  If not, there is a race condition in this code (find-then-save).
    public void createOrUpdateFromScanResult(
            String tenantId, Node node, IpInterfaceResult result, Map<Integer, SnmpInterface> ifIndexSNMPMap) {
        modelRepo
                .findByNodeIdAndTenantIdAndIpAddress(
                        node.getId(), tenantId, InetAddressUtils.getInetAddress(result.getIpAddress()))
                .ifPresentOrElse(
                        ipInterface -> {
                            ipInterface.setHostname(result.getIpHostName());
                            ipInterface.setNetmask(result.getNetmask());
                            var snmpInterface = ifIndexSNMPMap.get(result.getIfIndex());
                            if (snmpInterface != null) {
                                ipInterface.setSnmpInterface(snmpInterface);
                            }
                            ipInterface.setIfIndex(result.getIfIndex());
                            modelRepo.save(ipInterface);
                        },
                        () -> {
                            IpInterface ipInterface = mapper.fromScanResult(result);

                            ipInterface.setNode(node);
                            ipInterface.setTenantId(tenantId);
                            ipInterface.setSnmpPrimary(false);
                            ipInterface.setHostname(result.getIpHostName());
                            ipInterface.setIfIndex(result.getIfIndex());
                            var snmpInterface = ifIndexSNMPMap.get(result.getIfIndex());
                            if (snmpInterface != null) {
                                ipInterface.setSnmpInterface(snmpInterface);
                            }
                            modelRepo.save(ipInterface);
                        });
    }
}
