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

package org.opennms.horizon.server.metrics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.server.model.TimeRangeUnit;
import org.opennms.horizon.server.service.metrics.TSDBMetricsService;

import java.time.Duration;

public class DurationRangeTest {


    @Test
    public void testDuration() {

        var duration = TSDBMetricsService.getDuration(24, TimeRangeUnit.HOUR);
        Assertions.assertEquals(Duration.ofHours(24), duration.get());
        duration = TSDBMetricsService.getDuration(60, TimeRangeUnit.MINUTE);
        Assertions.assertEquals(Duration.ofMinutes(60), duration.get());
        duration = TSDBMetricsService.getDuration(30, TimeRangeUnit.SECOND);
        Assertions.assertEquals(Duration.ofSeconds(30), duration.get());
        duration = TSDBMetricsService.getDuration(2, TimeRangeUnit.DAY);
        Assertions.assertEquals(Duration.ofDays(2), duration.get());
    }
}
