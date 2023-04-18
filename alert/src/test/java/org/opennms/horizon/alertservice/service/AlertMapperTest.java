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

package org.opennms.horizon.alertservice.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.opennms.horizon.alertservice.db.entity.Alert;

public class AlertMapperTest {

    @Test
    public void canMapAlert() {
        Alert dbAlert = new Alert();
        dbAlert.setTenantId("wow1");
        dbAlert.setId(42L);
        dbAlert.setCounter(3L);
        dbAlert.setReductionKey("oops:1");
        dbAlert.setClearKey("clear:oops:1");
        dbAlert.setLastEventTime(new Date(49L));
        dbAlert.setMonitoringPolicyId(List.of(1L));

        var protoAlert = AlertMapper.INSTANCE.toProto(dbAlert);
        assertThat(protoAlert.getTenantId(), equalTo(dbAlert.getTenantId()));
        assertThat(protoAlert.getDatabaseId(), equalTo(dbAlert.getId()));
        assertThat(protoAlert.getCounter(), equalTo(dbAlert.getCounter()));
        assertThat(protoAlert.getReductionKey(), equalTo(dbAlert.getReductionKey()));
        assertThat(protoAlert.getClearKey(), equalTo(dbAlert.getClearKey()));
        assertThat(protoAlert.getLastUpdateTimeMs(), equalTo(dbAlert.getLastEventTime().getTime()));
        assertThat(protoAlert.getMonitoringPolicyIdList(), equalTo(List.of(1L)));
        assertThat(protoAlert.getIsAcknowledged(), equalTo(false));
    }

    @Test
    public void canMapAcknowledgedAlert() {
        Alert dbAlert = new Alert();
        dbAlert.setAcknowledgedByUser("me");
        dbAlert.setAcknowledgedAt(new Date());

        var protoAlert = AlertMapper.INSTANCE.toProto(dbAlert);
        assertThat(protoAlert.getIsAcknowledged(), equalTo(true));
        assertThat(protoAlert.getAckUser(), equalTo(dbAlert.getAcknowledgedByUser()));
        assertThat(protoAlert.getAckTimeMs(), equalTo(dbAlert.getAcknowledgedAt().getTime()));
    }

}
