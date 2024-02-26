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
package org.opennms.horizon.server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.opennms.horizon.inventory.dto.SnmpInterfaceDTO;
import org.opennms.horizon.server.model.inventory.SnmpInterface;
import org.opennms.horizon.server.model.inventory.SnmpInterfaceAdminStatus;
import org.opennms.horizon.server.model.inventory.SnmpInterfaceOperatorStatus;

@Mapper(componentModel = "spring")
public interface SnmpInterfaceMapper {
    @Mapping(source = "ifAdminStatus", target = "ifAdminStatus", qualifiedByName = "mapAdminStatus")
    @Mapping(source = "ifOperatorStatus", target = "ifOperatorStatus", qualifiedByName = "mapOperatorStatus")
    SnmpInterface protobufToSnmpInterface(SnmpInterfaceDTO protoBuf);

    @Named("mapAdminStatus")
    default String mapAdminStatus(int ifAdminStatus) {
        var status = SnmpInterfaceAdminStatus.valueOf(ifAdminStatus);
        return status != null ? status.name() : null;
    }

    @Named("mapOperatorStatus")
    default String mapOperatorStatus(int ifOperatorStatus) {
        var status = SnmpInterfaceOperatorStatus.valueOf(ifOperatorStatus);
        return status != null ? status.name() : null;
    }
}
