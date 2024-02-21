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
import org.mapstruct.Mappings;
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.node.scan.contract.IpInterfaceResult;

@Mapper(
        componentModel = "spring",
        uses = {EmptyStringMapper.class, AzureInterfaceMapper.class, IpAddressMapper.class})
public interface IpInterfaceMapper {

    @Mappings({
        @Mapping(target = "netmask", source = "netmask", qualifiedByName = "emptyString"),
        @Mapping(target = "tenantId", source = "tenantId", qualifiedByName = "emptyString"),
        @Mapping(target = "hostname", source = "hostname", qualifiedByName = "emptyString")
    })
    IpInterface dtoToModel(IpInterfaceDTO dto);

    @BeanMapping(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
    IpInterfaceDTO modelToDTO(IpInterface model);

    @Mappings({
        @Mapping(target = "netmask", source = "netmask", qualifiedByName = "emptyString"),
        @Mapping(target = "hostname", source = "ipHostName", qualifiedByName = "emptyString")
    })
    IpInterface fromScanResult(IpInterfaceResult result);
}
