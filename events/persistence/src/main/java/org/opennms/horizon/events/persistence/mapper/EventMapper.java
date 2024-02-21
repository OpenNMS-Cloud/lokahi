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
package org.opennms.horizon.events.persistence.mapper;

import com.google.protobuf.InvalidProtocolBufferException;
import java.net.InetAddress;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.opennms.horizon.events.persistence.model.Event;
import org.opennms.horizon.events.persistence.model.EventParameter;
import org.opennms.horizon.events.persistence.model.EventParameters;
import org.opennms.horizon.shared.utils.InetAddressUtils;

@Mapper(componentModel = "spring")
public interface EventMapper extends DateTimeMapper {

    @Mapping(source = "eventUei", target = "uei")
    @Mapping(source = "producedTime", target = "producedTimeMs")
    @Mapping(source = "eventInfo", target = "info")
    @Mapping(source = "id", target = "databaseId")
    org.opennms.horizon.events.proto.Event modelToDTO(Event event);

    default org.opennms.horizon.events.proto.Event modelToDtoWithParams(Event event) {
        org.opennms.horizon.events.proto.Event eventDTO = modelToDTO(event);

        org.opennms.horizon.events.proto.Event.Builder builder =
                org.opennms.horizon.events.proto.Event.newBuilder(eventDTO);

        EventParameters eventParams = event.getEventParameters();
        if (eventParams != null) {

            List<EventParameter> parameters = eventParams.getParameters();
            for (EventParameter param : parameters) {
                builder.addParameters(modelToDTO(param));
            }
        }
        return builder.build();
    }

    org.opennms.horizon.events.proto.EventParameter modelToDTO(EventParameter param);

    default org.opennms.horizon.events.proto.EventInfo map(byte[] value) {
        try {
            return org.opennms.horizon.events.proto.EventInfo.parseFrom(value);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }

    default String map(InetAddress value) {
        if (value == null) {
            return "";
        } else {
            return InetAddressUtils.toIpAddrString(value);
        }
    }
}
