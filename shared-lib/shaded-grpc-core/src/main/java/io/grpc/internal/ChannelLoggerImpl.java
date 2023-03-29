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

package io.grpc.internal;

import io.grpc.ChannelLogger;
import io.grpc.InternalLogId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;

/**
 * Replacement for the built-in ChannelLoggerImpl in grpc-core which ends up sending all log messages of interest to
 *  Java Utils Logging at TRACE level.
 *
 * Note this is a work-around to get the GRPC channel log messages at more reasonable levels (most are INFO as of this
 *  writing).
 */
public class ChannelLoggerImpl extends ChannelLogger {

    private static final Logger LOG = LoggerFactory.getLogger(ChannelLoggerImpl.class);

    ChannelLoggerImpl(ChannelTracer tracer, TimeProvider time) {
    }

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

    static void logOnly(
        InternalLogId logId, ChannelLogLevel level, String messageFormat, Object... args) {

        String msg = MessageFormat.format(messageFormat, args);

        switch (level) {
            case DEBUG -> LOG.debug("[{}] {}", logId, msg);
            case INFO -> LOG.info("[{}] {}", logId, msg);
            case WARNING -> LOG.warn("[{}] {}", logId, msg);
            case ERROR -> LOG.error("[{}] {}", logId, msg);
        }
    }

//========================================
// Internals
//----------------------------------------

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
