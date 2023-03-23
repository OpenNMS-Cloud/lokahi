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

import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * HS-1284
 *
 * The main purpose of this class is to wait for the connection to the JDBC database used by Ignite at bean-init-time,
 *  in a bean that the Ignite bean depends on, so that Ignite does not start until the database is available.
 *
 * This works-around the fact that Ignite only attempts to connect to the database one time at startup in order to
 *  initialize the schema, and if that fails, ignite's cache persistence will never recover.
 */
@Component("igniteJdbcConnectionStartupGate")
public class JdbcInitComponent {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcInitComponent.class);

    public static final int DEFAULT_TIMEOUT = 60_000;
    public static final int DEFAULT_RETRY_PERIOD = 500;

    @Value("${ignite-jdbc.connection.startup-gate.timeout:" + DEFAULT_TIMEOUT + "}")
    @Setter
    private int timeout;

    @Value("${ignite-jdbc.connection.startup-gate.retry-period:" + DEFAULT_RETRY_PERIOD + "}")
    @Setter
    private long retryPeriod;

    @Setter
    private Supplier<Long> timestampClockSource = System::nanoTime;
    @Setter
    private Consumer<Long> delayOperation = this::delay;

    private final DataSource dataSource;

    private Exception lastException;

//========================================
// Constructor
//----------------------------------------

    public JdbcInitComponent(DataSource dataSource) {
        this.dataSource = dataSource;
    }

//========================================
// Initialization
//----------------------------------------

    @PostConstruct
    public void init() {
        long startTimestamp = timestampClockSource.get();
        long now = startTimestamp;
        boolean connected = false;
        boolean first = true;
        int count = 0;

        while (
            (! connected) &&
            (! isTimedOut(startTimestamp, now, timeout))
        ) {
            if (first) {
                first = false;
            } else {
                delayOperation.accept(retryPeriod);
            }

            count++;
            connected = attemptConnect();
            now = timestampClockSource.get();
        }

        if (!connected) {
            LOG.error("Timed out attempting to connect to the database; aborting startup: connection-attempt-count={}; timeout={}", count, timeout);
            throw new RuntimeException("Timed out attempting to connect to the database; aborting startup", lastException);
        }
    }

//========================================
// Internals
//----------------------------------------

    private boolean attemptConnect() {
        try {
            Connection connection = this.dataSource.getConnection();
            connection.close();

            return true;
        } catch (Exception exc) {
            LOG.info("Failed to connect to database", exc);
            lastException = exc;
        }

        return false;
    }

    private void delay(long period) {
        try {
            Thread.sleep(period);
        } catch (InterruptedException intExc) {
            LOG.debug("Interrupted during delay", intExc);
        }
    }

    /**
     * Check for timeout.
     *
     * @param startupTimestamp time at startup in nanoseconds
     * @param now current time in nanoseconds
     * @param timeoutMs timeout period in milliseconds
     *
     * @return true => timeout exceeded; false => timeout not yet exceeded.
     */
    private boolean isTimedOut(long startupTimestamp, long now, long timeoutMs) {
        long delta = now - startupTimestamp;
        return (delta > (timeoutMs * 1_000_000L));
    }
}
