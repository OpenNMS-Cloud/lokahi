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
        assertThat(
                protoAlert.getLastUpdateTimeMs(),
                equalTo(dbAlert.getLastEventTime().getTime()));
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
        assertThat(
                protoAlert.getAckTimeMs(), equalTo(dbAlert.getAcknowledgedAt().getTime()));
    }
}
