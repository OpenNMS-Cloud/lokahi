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

import java.util.List;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryCreateDTO;
import org.opennms.horizon.inventory.discovery.IcmpActiveDiscoveryDTO;
import org.opennms.horizon.inventory.discovery.SNMPConfigDTO;
import org.opennms.horizon.server.mapper.TagMapper;
import org.opennms.horizon.server.model.inventory.discovery.SNMPConfig;
import org.opennms.horizon.server.model.inventory.discovery.active.IcmpActiveDiscovery;
import org.opennms.horizon.server.model.inventory.discovery.active.IcmpActiveDiscoveryCreate;

@Mapper(
        componentModel = "spring",
        uses = {TagMapper.class},
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public interface IcmpActiveDiscoveryMapper {

    @Mapping(source = "readCommunityList", target = "readCommunities")
    @Mapping(source = "portsList", target = "ports")
    SNMPConfig snmpDtoToModel(SNMPConfigDTO snmpDto);

    @Mapping(source = "readCommunities", target = "readCommunityList")
    @Mapping(source = "ports", target = "portsList")
    SNMPConfigDTO snmpConfigToDTO(SNMPConfig snmpConfig);

    @Mapping(target = "ipAddressesList", source = "ipAddresses")
    @Mapping(target = "snmpConfig", source = "snmpConfig", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    @Mapping(target = "tagsList", source = "tags", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    IcmpActiveDiscoveryCreateDTO mapRequest(IcmpActiveDiscoveryCreate request);

    @Mapping(source = "ipAddressesList", target = "ipAddresses")
    @Mapping(target = "snmpConfig", source = "snmpConfig", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    IcmpActiveDiscovery dtoToIcmpActiveDiscovery(IcmpActiveDiscoveryDTO configDTO);

    List<IcmpActiveDiscovery> dtoListToIcmpActiveDiscoveryList(List<IcmpActiveDiscoveryDTO> dtoList);
}
