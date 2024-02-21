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
package org.opennms.horizon.alertservice.mapper;

import java.util.Date;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.factory.Mappers;
import org.opennms.horizon.alerts.proto.ManagedObject;
import org.opennms.horizon.alerts.proto.ManagedObjectInstance;
import org.opennms.horizon.alerts.proto.ManagedObjectType;
import org.opennms.horizon.alerts.proto.NodeRef;
import org.opennms.horizon.alerts.proto.SnmpInterfaceRef;
import org.opennms.horizon.alertservice.db.entity.Alert;

@Mapper(
        componentModel = "spring",
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface AlertMapper {

    AlertMapper INSTANCE = Mappers.getMapper(AlertMapper.class);

    @Mapping(target = "databaseId", source = "id")
    @Mapping(target = "uei", source = "eventUei")
    @Mapping(target = "firstEventTimeMs", source = "firstEventTime")
    @Mapping(target = "lastUpdateTimeMs", source = "lastEventTime")
    @Mapping(target = "isAcknowledged", expression = "java(alert.getAcknowledgedByUser() != null ? true : false)")
    @Mapping(target = "ackUser", source = "acknowledgedByUser")
    @Mapping(target = "ackTimeMs", source = "acknowledgedAt")
    @Mapping(target = "monitoringPolicyIdList", source = "monitoringPolicyId")
    @Mapping(target = "label", source = "alertCondition.triggerEvent.name")
    @Mapping(target = "nodeName", source = "nodeLabel")
    @Mapping(target = "managedObject", expression = "java(mapAlertToMangedObject(alert))")
    org.opennms.horizon.alerts.proto.Alert toProto(Alert alert);

    default long mapDateToLongMs(Date value) {
        return value == null ? 0L : value.getTime();
    }

    default ManagedObject mapAlertToMangedObject(Alert alert) {
        var managedObject = ManagedObject.newBuilder().setType(alert.getManagedObjectType());
        if (alert.getManagedObjectType() == ManagedObjectType.NODE) {
            managedObject.setInstance(ManagedObjectInstance.newBuilder()
                    .setNodeVal(NodeRef.newBuilder().setNodeId(Long.parseLong(alert.getManagedObjectInstance()))));
        } else if (alert.getManagedObjectType() == ManagedObjectType.SNMP_INTERFACE) {
            managedObject.setInstance(ManagedObjectInstance.newBuilder()
                    .setSnmpInterfaceVal(SnmpInterfaceRef.newBuilder()
                            .setIfIndex(Long.parseLong(alert.getManagedObjectInstance()))));
        }
        return managedObject.build();
    }
}
