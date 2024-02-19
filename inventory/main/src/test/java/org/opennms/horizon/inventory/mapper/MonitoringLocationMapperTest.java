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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.inventory.dto.GeoLocation;
import org.opennms.horizon.inventory.dto.MonitoringLocationDTO;
import org.opennms.horizon.inventory.model.MonitoringLocation;

@ExtendWith(MockitoExtension.class)
class MonitoringLocationMapperTest {

    @InjectMocks
    MonitoringLocationMapperImpl mapper;

    @Test
    void testProtoToLocation() {
        var proto = MonitoringLocationDTO.newBuilder()
                .setId(1L)
                .setTenantId("testTenantId")
                .setLocation("testLocationName")
                .setGeoLocation(GeoLocation.newBuilder()
                        .setLatitude(1.0)
                        .setLongitude(2.0)
                        .build())
                .setAddress("address")
                .build();
        var result = mapper.dtoToModel(proto);
        assertEquals(1L, result.getId());
        assertEquals("testLocationName", result.getLocation());
        assertEquals(1.0, result.getLatitude());
        assertEquals(2.0, result.getLongitude());
        assertEquals(proto.getTenantId(), result.getTenantId());
        assertEquals(proto.getAddress(), result.getAddress());
    }

    @Test
    void testLocationToProto() {
        var model = new MonitoringLocation();
        model.setId(1L);
        model.setLocation("testLocationName");
        model.setLatitude(1.0);
        model.setLongitude(2.0);
        model.setTenantId("testTenantId");
        var result = mapper.modelToDTO(model);
        assertEquals(1L, result.getId());
        assertEquals("testLocationName", result.getLocation());
        assertEquals(1.0, result.getGeoLocation().getLatitude());
        assertEquals(2.0, result.getGeoLocation().getLongitude());
        assertEquals("testTenantId", result.getTenantId());
    }
}
