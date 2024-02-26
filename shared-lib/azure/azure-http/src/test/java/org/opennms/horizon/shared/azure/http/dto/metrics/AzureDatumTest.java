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
package org.opennms.horizon.shared.azure.http.dto.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import org.junit.Test;

public class AzureDatumTest {

    @Test
    public void testDatumWithTotal() {
        AzureDatum datumWithTotal = getDatumWithTotal();
        assertEquals(123d, datumWithTotal.getValue(), 0d);
        assertEquals(123d, datumWithTotal.getTotal(), 0d);
        assertNull(datumWithTotal.getAverage());
    }

    @Test
    public void testDatumWithAverage() {
        AzureDatum datumWithAverage = getDatumWithAverage();
        assertEquals(456d, datumWithAverage.getValue(), 0d);
        assertEquals(456d, datumWithAverage.getAverage(), 0d);
        assertNull(datumWithAverage.getTotal());
    }

    private AzureDatum getDatumWithTotal() {
        AzureDatum datum = new AzureDatum();
        datum.setTimeStamp(Instant.now().toString());
        datum.setTotal(123d);
        return datum;
    }

    private AzureDatum getDatumWithAverage() {
        AzureDatum datum = new AzureDatum();
        datum.setTimeStamp(Instant.now().toString());
        datum.setAverage(456d);
        return datum;
    }
}
