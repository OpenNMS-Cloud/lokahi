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
package org.opennms.horizon.server.mapper.alert;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.opennms.horizon.server.model.alerts.*;

@Mapper(componentModel = "spring")
public interface AlertMapper {

    @Mapping(source = "alertsList", target = "alerts")
    ListAlertResponse protoToAlertResponse(org.opennms.horizon.alerts.proto.ListAlertsResponse listAlertsResponse);

    @Mapping(source = "isAcknowledged", target = "acknowledged")
    Alert protoToAlert(org.opennms.horizon.alerts.proto.Alert alertProto);

    @Mapping(source = "alertList", target = "alertList")
    @Mapping(source = "alertErrorList", target = "alertErrorList")
    AlertResponse protoToAlertResponse(org.opennms.horizon.alerts.proto.AlertResponse alertResponse);

    @Mapping(source = "alertIdList", target = "alertDatabaseIdList")
    @Mapping(source = "alertErrorList", target = "alertErrorList")
    DeleteAlertResponse protoToDeleteAlertResponse(
            org.opennms.horizon.alerts.proto.DeleteAlertResponse deleteAlertResponse);

    @Mapping(source = "alertId", target = "databaseId")
    AlertError protoToAlertError(org.opennms.horizon.alerts.proto.AlertError alertError);

    CountAlertResponse protoToCountAlertResponse(
            org.opennms.horizon.alerts.proto.CountAlertResponse countAlertResponse);

    String alertErrorToString(org.opennms.horizon.alerts.proto.AlertError alertError);
}
