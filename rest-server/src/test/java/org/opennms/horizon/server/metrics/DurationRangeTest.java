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
package org.opennms.horizon.server.metrics;

import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.server.model.TimeRangeUnit;
import org.opennms.horizon.server.service.metrics.QueryService;

public class DurationRangeTest {

    @Test
    public void testDuration() {

        var duration = QueryService.getDuration(24, TimeRangeUnit.HOUR);
        Assertions.assertEquals(Duration.ofHours(24), duration.get());
        duration = QueryService.getDuration(60, TimeRangeUnit.MINUTE);
        Assertions.assertEquals(Duration.ofMinutes(60), duration.get());
        duration = QueryService.getDuration(30, TimeRangeUnit.SECOND);
        Assertions.assertEquals(Duration.ofSeconds(30), duration.get());
        duration = QueryService.getDuration(2, TimeRangeUnit.DAY);
        Assertions.assertEquals(Duration.ofDays(2), duration.get());
    }
}
