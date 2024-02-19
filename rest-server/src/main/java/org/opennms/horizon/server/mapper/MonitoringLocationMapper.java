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
import org.mapstruct.NullValueCheckStrategy;
import org.opennms.horizon.inventory.dto.MonitoringLocationCreateDTO;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.server.model.inventory.MonitoringLocation;
import org.opennms.horizon.server.model.inventory.MonitoringLocationCreate;
import org.opennms.horizon.server.model.inventory.MonitoringLocationUpdate;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface MonitoringLocationMapper {
    @Mapping(target = "geoLocation.latitude", source = "latitude")
    @Mapping(target = "geoLocation.longitude", source = "longitude")
    MonitoringLocationCreateDTO locationCreateToLocationCreateProto(MonitoringLocationCreate location);

    @Mapping(target = "geoLocation.latitude", source = "latitude")
    @Mapping(target = "geoLocation.longitude", source = "longitude")
    MonitoringLocationDTO locationUpdateToLocationProto(MonitoringLocationUpdate location);

    @Mapping(target = "latitude", source = "geoLocation.latitude")
    @Mapping(target = "longitude", source = "geoLocation.longitude")
    MonitoringLocation protoToLocation(MonitoringLocationDTO location);
}
