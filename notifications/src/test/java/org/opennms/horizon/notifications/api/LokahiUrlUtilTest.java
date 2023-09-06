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

package org.opennms.horizon.notifications.api;

import org.junit.Assert;
import org.junit.Test;
import org.opennms.horizon.alerts.proto.Alert;
import org.springframework.test.util.ReflectionTestUtils;

public class LokahiUrlUtilTest {
    private final Alert alert = Alert.newBuilder().setTenantId("tenantId").build();

    private final LokahiUrlUtil lokahiUrlUtil = new LokahiUrlUtil();
    @Test
    public void testAppendFalse(){
        ReflectionTestUtils.setField(lokahiUrlUtil, "baseUrl", "onmshs.local:1443");
        ReflectionTestUtils.setField(lokahiUrlUtil, "urlAppendTenantId", false);
        String url = lokahiUrlUtil.getAlertstUrl(alert);
        Assert.assertEquals("https://onmshs.local:1443/alerts", url);
    }

    @Test
    public void testAppendTrue(){
        ReflectionTestUtils.setField(lokahiUrlUtil, "baseUrl", "opennms.com");
        ReflectionTestUtils.setField(lokahiUrlUtil, "urlAppendTenantId", true);
        String url = lokahiUrlUtil.getAlertstUrl(alert);
        Assert.assertEquals("https://tenantId.opennms.com/alerts", url);
    }
}
