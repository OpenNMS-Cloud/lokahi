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
package org.opennms.horizon.inventory.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.opennms.horizon.azure.api.AzureScanNetworkInterfaceItem;
import org.opennms.horizon.inventory.dto.AzureInterfaceDTO;
import org.opennms.horizon.inventory.model.AzureInterface;

@Mapper(
        componentModel = "spring",
        uses = {EmptyStringMapper.class, IpAddressMapper.class})
public interface AzureInterfaceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicIpAddress", source = "publicIpAddress.ipAddress", qualifiedByName = "emptyString")
    @Mapping(target = "privateIpId", source = "name", qualifiedByName = "emptyString")
    @Mapping(target = "publicIpId", source = "publicIpAddress.name", qualifiedByName = "emptyString")
    @Mapping(target = "interfaceName", source = "interfaceName", qualifiedByName = "emptyString")
    @Mapping(target = "location", source = "location", qualifiedByName = "emptyString")
    AzureInterface scanResultToModel(AzureScanNetworkInterfaceItem scanResult);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "publicIpAddress", source = "publicIpAddress.ipAddress", qualifiedByName = "emptyString")
    @Mapping(target = "privateIpId", source = "name", qualifiedByName = "emptyString")
    @Mapping(target = "publicIpId", source = "publicIpAddress.name", qualifiedByName = "emptyString")
    @Mapping(target = "interfaceName", source = "interfaceName", qualifiedByName = "emptyString")
    void updateFromScanResult(@MappingTarget AzureInterface result, AzureScanNetworkInterfaceItem scanResult);

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    AzureInterfaceDTO modelToDTO(AzureInterface model);

    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
            nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    AzureInterface dtoToModel(AzureInterfaceDTO model);
}
