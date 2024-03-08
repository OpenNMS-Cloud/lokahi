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
package io.grpc.internal;

import io.grpc.ChannelLogger;
import io.grpc.InternalLogId;
import java.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Replacement for the built-in ChannelLoggerImpl in grpc-core which ends up sending all log messages of interest to
 *  Java Utils Logging at TRACE level.
 *
 * Note this is a work-around to get the GRPC channel log messages at more reasonable levels (most are INFO as of this
 *  writing).
 */
public class ChannelLoggerImpl extends ChannelLogger {

    private static final Logger LOG = LoggerFactory.getLogger(ChannelLoggerImpl.class);

    ChannelLoggerImpl(ChannelTracer tracer, TimeProvider time) {}

    @Override
    public void log(ChannelLogLevel level, String message) {
        switch (level) {
            case DEBUG -> LOG.debug("{}", message);
            case INFO -> LOG.info("{}", message);
            case WARNING -> LOG.warn("{}", message);
            case ERROR -> LOG.error("{}", message);
        }
    }

    @Override
    public void log(ChannelLogLevel level, String messageFormat, Object... args) {
        String slf4jMsg = julFormatToSlf4jFormat(messageFormat);

        switch (level) {
            case DEBUG -> LOG.debug(slf4jMsg, args);
            case INFO -> LOG.info(slf4jMsg, args);
            case WARNING -> LOG.warn(slf4jMsg, args);
            case ERROR -> LOG.error(slf4jMsg, args);
        }
    }

    static void logOnly(InternalLogId logId, ChannelLogLevel level, String msg) {
        switch (level) {
            case DEBUG -> LOG.debug("[{}] {}", logId, msg);
            case INFO -> LOG.info("[{}] {}", logId, msg);
            case WARNING -> LOG.warn("[{}] {}", logId, msg);
            case ERROR -> LOG.error("[{}] {}", logId, msg);
        }
    }

    static void logOnly(InternalLogId logId, ChannelLogLevel level, String messageFormat, Object... args) {

        String msg = MessageFormat.format(messageFormat, args);

        switch (level) {
            case DEBUG -> LOG.debug("[{}] {}", logId, msg);
            case INFO -> LOG.info("[{}] {}", logId, msg);
            case WARNING -> LOG.warn("[{}] {}", logId, msg);
            case ERROR -> LOG.error("[{}] {}", logId, msg);
        }
    }

    // ========================================
    // Internals
    // ----------------------------------------

    /**
     * Convert the JUL message format, which uses "{0}" for an argument placeholder, with the SLF4J format, which uses
     * "{}" for an argument placeholder.
     *
     * @param julFormat the JUL message format with argument placeholder in the format "{0}"
     * @return SL4J message format with argument placeholder "{}"
     */
    private static String julFormatToSlf4jFormat(String julFormat) {
        return julFormat.replaceAll("\\{\\d\\}", "{}");
    }
}
