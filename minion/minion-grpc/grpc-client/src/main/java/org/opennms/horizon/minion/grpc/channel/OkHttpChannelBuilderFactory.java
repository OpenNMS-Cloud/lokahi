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

package org.opennms.horizon.minion.grpc.channel;

import io.grpc.ChannelCredentials;
import io.grpc.ManagedChannelBuilder;
import io.grpc.okhttp.OkHttpChannelBuilder;
import lombok.Setter;

public class OkHttpChannelBuilderFactory implements ChannelBuilderFactory {

    @Setter
    private int maxInboundMessageSize = 0;

    @Setter
    private String imageCreated = "";

    @Setter
    private String imageVersion = "";

    @Setter
    private String gitDescribe = "";

    @Setter
    private String gitBranch = "";

    public String getUserAgent() {
        final var name = "OpenNMS_Lokahi_Minion";

        final String version;
        if (!imageVersion.isBlank()) {
            version = String.format("%s/%s (%s)", name, imageVersion, gitDescribe);
        } else {
            version = String.format("%s/%s (built %s; branch %s)", name, gitDescribe, imageCreated, gitBranch);
        }

        return String.format("%s %s/%s (%s/%s; %s) %s/%s (%s)",
            version,
            "Java", System.getProperty("java.version"),
            System.getProperty("java.vm.name"), System.getProperty("java.vm.version"), System.getProperty("java.vm.vendor"),
            System.getProperty("os.name").replaceAll("\\s+", "_"), System.getProperty("os.version"), System.getProperty("os.arch")
            );
    }

    @Override
    public ManagedChannelBuilder<?> create(String host, int port, String authority, ChannelCredentials credentials) {
        final OkHttpChannelBuilder channelBuilder;
        if (credentials == null) {
            channelBuilder = OkHttpChannelBuilder.forAddress(host, port);
        } else {
            channelBuilder = OkHttpChannelBuilder.forAddress(host, port, credentials);
        }
        channelBuilder.userAgent(getUserAgent());
        channelBuilder.keepAliveWithoutCalls(true);
        if (maxInboundMessageSize != 0) {
            channelBuilder.maxInboundMessageSize(maxInboundMessageSize);
        }

        // If an override authority was specified, configure it now.  Setting the override authority to match the CN
        //  of the server certificate prevents hostname verification errors of the server certificate when the CN does
        //  not match the hostname used to connect the server.
        if (authority != null && !authority.isBlank()) {
            channelBuilder.overrideAuthority(authority);
        }

        return channelBuilder;
    }
}
