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
import org.mapstruct.Mappings;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.inventory.dto.SnmpInterfaceDTO;
import org.opennms.horizon.inventory.model.SnmpInterface;
import org.opennms.node.scan.contract.SnmpInterfaceResult;

@Mapper(
        componentModel = "spring",
        uses = {EmptyStringMapper.class, IpAddressMapper.class})
public interface SnmpInterfaceMapper {
    @Mappings({
        @Mapping(target = "tenantId", source = "tenantId", qualifiedByName = "emptyString"),
        @Mapping(target = "ifDescr", source = "ifDescr", qualifiedByName = "emptyString"),
        @Mapping(target = "ifName", source = "ifName", qualifiedByName = "emptyString"),
        @Mapping(target = "ifAlias", source = "ifAlias", qualifiedByName = "emptyString"),
        @Mapping(target = "physicalAddr", source = "physicalAddr", qualifiedByName = "emptyString")
    })
    SnmpInterface dtoToModel(SnmpInterfaceDTO dto);

    @BeanMapping(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    SnmpInterfaceDTO modelToDTO(SnmpInterface model);

    @Mappings({
        @Mapping(target = "ifDescr", source = "ifDescr", qualifiedByName = "emptyString"),
        @Mapping(target = "ifName", source = "ifName", qualifiedByName = "emptyString"),
        @Mapping(target = "ifAlias", source = "ifAlias", qualifiedByName = "emptyString"),
        @Mapping(target = "physicalAddr", source = "physicalAddr", qualifiedByName = "emptyString")
    })
    SnmpInterface scanResultToModel(SnmpInterfaceResult result);

    @Mappings({
        @Mapping(target = "ifDescr", source = "ifDescr", qualifiedByName = "emptyString"),
        @Mapping(target = "ifName", source = "ifName", qualifiedByName = "emptyString"),
        @Mapping(target = "ifAlias", source = "ifAlias", qualifiedByName = "emptyString"),
        @Mapping(target = "physicalAddr", source = "physicalAddr", qualifiedByName = "emptyString")
    })
    void updateFromScanResult(SnmpInterfaceResult result, @MappingTarget SnmpInterface snmpInterface);
}
