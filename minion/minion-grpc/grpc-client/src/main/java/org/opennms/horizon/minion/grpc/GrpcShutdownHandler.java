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

package org.opennms.horizon.minion.grpc;

import org.apache.karaf.system.SystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcShutdownHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcShutdownHandler.class);

    private final SystemService systemService;

    public GrpcShutdownHandler(SystemService systemService) {
        this.systemService = systemService;
    }

    public void shutdown(Throwable throwable) {
        shutdown(String.format("%s. Going to shut down now.", throwable.getMessage()));
    }

    public void shutdown(String message) {
        try {
            LOG.error(message);
            systemService.halt();
        } catch (Exception e) {
            LOG.error("Fail to shutdown properly. Calling system.exit now. Error: {}", e.getMessage());
            System.exit(-1);
        }
    }
}
