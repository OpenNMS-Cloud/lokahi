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
package org.opennms.horizon.inventory.mapper.discovery;

import org.mapstruct.AfterMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryDTO;
import org.opennms.horizon.inventory.model.discovery.active.IcmpActiveDiscovery;

@Mapper(componentModel = "spring", collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface IcmpActiveDiscoveryMapper {

    @Mapping(target = "ipAddressEntries", source = "ipAddressesList")
    @Mapping(
            target = "snmpCommunityStrings",
            source = "snmpConfig.readCommunityList",
            nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(
            target = "snmpPorts",
            source = "snmpConfig.portsList",
            nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    IcmpActiveDiscovery dtoToModel(IcmpActiveDiscoveryCreateDTO dto);

    @Mapping(target = "ipAddressesList", source = "ipAddressEntries")
    @Mapping(
            target = "snmpConfig.readCommunityList",
            source = "snmpCommunityStrings",
            nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(
            target = "snmpConfig.portsList",
            source = "snmpPorts",
            nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    IcmpActiveDiscoveryDTO modelToDto(IcmpActiveDiscovery discovery);

    @AfterMapping
    default void trimName(@MappingTarget IcmpActiveDiscovery icmpActiveDiscovery) {
        var trimmedName = icmpActiveDiscovery.getName().trim();
        icmpActiveDiscovery.setName(trimmedName);
    }

    @Mapping(target = "ipAddressEntries", source = "ipAddressesList")
    @Mapping(
            target = "snmpCommunityStrings",
            source = "snmpConfig.readCommunityList",
            nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(
            target = "snmpPorts",
            source = "snmpConfig.portsList",
            nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    IcmpActiveDiscovery dtoToModel(IcmpActiveDiscoveryDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "ipAddressEntries", source = "ipAddressesList")
    @Mapping(
            target = "snmpCommunityStrings",
            source = "snmpConfig.readCommunityList",
            nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(
            target = "snmpPorts",
            source = "snmpConfig.portsList",
            nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    void updateFromDto(IcmpActiveDiscoveryCreateDTO dto, @MappingTarget IcmpActiveDiscovery discovery);
}
