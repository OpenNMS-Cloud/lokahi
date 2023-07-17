/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

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
    DeleteAlertResponse protoToDeleteAlertResponse(org.opennms.horizon.alerts.proto.DeleteAlertResponse deleteAlertResponse);

    @Mapping(source = "alertId", target = "databaseId")
    AlertError protoToAlertError(org.opennms.horizon.alerts.proto.AlertError alertError);

    CountAlertResponse protoToCountAlertResponse(org.opennms.horizon.alerts.proto.CountAlertResponse countAlertResponse);

    String alertErrorToString(org.opennms.horizon.alerts.proto.AlertError alertError);
}
