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
package org.opennms.miniongateway.jdbc;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class JdbcInitComponentTest {

    private JdbcInitComponent target;

    private DataSource mockDataSource;
    private Connection mockConnection;

    @Before
    public void setUp() throws Exception {
        mockDataSource = Mockito.mock(DataSource.class);
        mockConnection = Mockito.mock(Connection.class);

        target = new JdbcInitComponent(mockDataSource);
    }

    @Test
    public void testInitSuccessFirstTry() throws SQLException {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockDataSource.getConnection()).thenReturn(mockConnection);

        //
        // Execute
        //
        target.init();

        //
        // Verify the Results
        //
        // NOTE: a lack of exception indicates success
        Mockito.verify(mockConnection).close();
    }

    @Test
    public void testInitSuccessThirdTry() throws SQLException {
        //
        // Setup Test Data and Interactions
        //
        SQLException testException = new SQLException("x-test-sql-exception-x");
        Mockito.when(mockDataSource.getConnection())
                .thenThrow(testException)
                .thenThrow(testException)
                .thenThrow(testException)
                .thenReturn(mockConnection);

        //
        // Execute
        //
        List<Long> delayPeriods = new LinkedList<>();
        target.setTimestampClockSource(
                prepareClockSource(1_000_000_000L, 1_000_000_000L, 2_000_000_000L, 3_000_000_000L, 4_000_000_000L));
        target.setDelayOperation(delay -> delayPeriods.add(delay));
        target.setTimeout(JdbcInitComponent.DEFAULT_TIMEOUT);
        target.init();

        //
        // Verify the Results
        //
        // NOTE: a lack of exception indicates success
        assertEquals(3, delayPeriods.size());
        Mockito.verify(mockConnection).close();
    }

    @Test
    public void testInitFailForthTry() throws SQLException {
        //
        // Setup Test Data and Interactions
        //
        SQLException testException = new SQLException("x-test-sql-exception-x");
        Mockito.when(mockDataSource.getConnection())
                .thenThrow(testException)
                .thenThrow(testException)
                .thenThrow(testException)
                .thenThrow(testException);

        //
        // Execute
        //
        List<Long> delayPeriods = new LinkedList<>();
        target.setTimestampClockSource(prepareClockSource(1_000_000_000L, 1_000_000_000L));
        target.setDelayOperation(delay -> delayPeriods.add(delay));
        target.setTimeout(4_000);

        Exception caught = null;
        try {
            target.init();
            fail("Missing expected exception");
        } catch (Exception thrown) {
            caught = thrown;
        }

        //
        // Verify the Results
        //
        // NOTE: a lack of exception indicates success
        assertSame(testException, caught.getCause());
        assertEquals(4, delayPeriods.size());
    }

    // ========================================
    //
    // ----------------------------------------

    private Supplier<Long> prepareClockSource(long defaultTickLength, long... ticks) {
        return new Supplier<Long>() {
            private int cur = 0;

            @Override
            public Long get() {
                if (cur >= ticks.length) {
                    cur++;
                    return ticks[ticks.length - 1] + (defaultTickLength * (cur - ticks.length));
                }

                return ticks[cur++];
            }
        };
    }
}
