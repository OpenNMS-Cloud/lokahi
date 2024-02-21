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
package org.opennms.horizon.minion.grpc;

import java.util.regex.Pattern;
import org.apache.karaf.system.SystemService;
import org.opennms.horizon.shared.logging.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcShutdownHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcShutdownHandler.class);
    private static final String LOG_PREFIX = "error";
    private static final Pattern pattern = Pattern.compile("^\\+\\d+$"); // "+2" - denotes 2 min wait before shutdown
    private static final String MINION_DELAY_BEFORE_SHUTDOWN_ENV = "MINION_DELAY_BEFORE_SHUTDOWN";
    private final SystemService systemService;

    public GrpcShutdownHandler(SystemService systemService) {
        this.systemService = systemService;
    }

    public void shutdown(Throwable throwable) {
        shutdown(String.format("%s. Going to shut down now.", throwable.getMessage()));
    }

    public void shutdown(String message) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.error(
                    "*********************************************************************************************************************************");
            LOG.error(
                    "*********************************************************************************************************************************");
            LOG.error(message);
            LOG.error(
                    "*********************************************************************************************************************************");
            LOG.error(
                    "*********************************************************************************************************************************");
        }));
        try (Logging.MDCCloseable mdc = Logging.withPrefixCloseable(LOG_PREFIX)) {
            LOG.error(
                    "*********************************************************************************************************************************");
            LOG.error(
                    "*********************************************************************************************************************************");
            LOG.error(message);
            LOG.error(
                    "*********************************************************************************************************************************");
            LOG.error(
                    "*********************************************************************************************************************************");
            var shutdownEnv = System.getenv(MINION_DELAY_BEFORE_SHUTDOWN_ENV);
            String delayForShutDown = "+0"; // no wait
            if (shutdownEnv != null) {
                var matcher = pattern.matcher(shutdownEnv);
                if (matcher.matches()) {
                    delayForShutDown = shutdownEnv;
                }
            }
            systemService.halt(delayForShutDown);

        } catch (Exception e) {
            LOG.error("Fail to shutdown properly. Calling system.exit now. Error: {}", e.getMessage());
            System.exit(-1);
        }
    }
}
