/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2014-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
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
package org.opennms.horizon.jmx.config;

import java.util.HashSet;
import java.util.Set;

public class JmxConfig {
    private Set<MBeanServer> mBeanServer = new HashSet<>();

    public Set<MBeanServer> getMBeanServer() {
        return mBeanServer;
    }

    public void setMBeanServer(Set<MBeanServer> mBeanServer) {
        this.mBeanServer = mBeanServer;
    }

    public MBeanServer lookupMBeanServer(String ipAddress, int port) {
        for (MBeanServer mBeanServer : getMBeanServer()) {
            if (port == mBeanServer.getPort() && ipAddress.equals(mBeanServer.getIpAddress()))
                return mBeanServer;
        }
        return null;
    }

    public MBeanServer lookupMBeanServer(String ipAddress, String port) {
        return lookupMBeanServer(ipAddress, Integer.parseInt(port));
    }
}
