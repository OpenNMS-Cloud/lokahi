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

import java.sql.Connection;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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

    // ========================================
    // Constructor
    // ----------------------------------------

    public JdbcInitComponent(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ========================================
    // Initialization
    // ----------------------------------------

    @PostConstruct
    public void init() {
        long startTimestamp = timestampClockSource.get();
        long now = startTimestamp;
        boolean connected = false;
        boolean first = true;
        int count = 0;

        while ((!connected) && (!isTimedOut(startTimestamp, now, timeout))) {
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
            LOG.error(
                    "Timed out attempting to connect to the database; aborting startup: connection-attempt-count={}; timeout={}",
                    count,
                    timeout);
            throw new RuntimeException(
                    "Timed out attempting to connect to the database; aborting startup", lastException);
        }
    }

    // ========================================
    // Internals
    // ----------------------------------------

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
