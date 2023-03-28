/*
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
 *
 */

package org.opennms.miniongateway.jdbc;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.*;

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
            prepareClockSource(1_000_000_000L, 1_000_000_000L, 2_000_000_000L, 3_000_000_000L, 4_000_000_000L)
        );
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
        target.setTimestampClockSource(
            prepareClockSource(1_000_000_000L, 1_000_000_000L)
        );
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

//========================================
//
//----------------------------------------

    private Supplier<Long> prepareClockSource(long defaultTickLength, long... ticks) {
        return new Supplier<Long>() {
            private int cur = 0;
            @Override
            public Long get() {
                if (cur >= ticks.length) {
                    cur++;
                    return ticks[ticks.length - 1] + ( defaultTickLength * ( cur - ticks.length ) );
                }

                return ticks[cur++];
            }
        };
    }

}
