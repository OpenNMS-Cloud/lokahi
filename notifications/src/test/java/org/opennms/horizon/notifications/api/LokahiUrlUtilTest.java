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
package org.opennms.horizon.notifications.api;

import org.junit.Assert;
import org.junit.Test;
import org.opennms.horizon.alerts.proto.Alert;
import org.springframework.test.util.ReflectionTestUtils;

public class LokahiUrlUtilTest {
    private final Alert alert = Alert.newBuilder().setTenantId("tenantId").build();

    private final LokahiUrlUtil lokahiUrlUtil = new LokahiUrlUtil();

    @Test
    public void testAppendFalse() {
        ReflectionTestUtils.setField(lokahiUrlUtil, "baseUrl", "onmshs.local:1443");
        ReflectionTestUtils.setField(lokahiUrlUtil, "urlAppendTenantId", false);
        String url = lokahiUrlUtil.getAlertstUrl(alert);
        Assert.assertEquals("https://onmshs.local:1443/alerts", url);
    }

    @Test
    public void testAppendTrue() {
        ReflectionTestUtils.setField(lokahiUrlUtil, "baseUrl", "opennms.com");
        ReflectionTestUtils.setField(lokahiUrlUtil, "urlAppendTenantId", true);
        String url = lokahiUrlUtil.getAlertstUrl(alert);
        Assert.assertEquals("https://tenantId.opennms.com/alerts", url);
    }
}
