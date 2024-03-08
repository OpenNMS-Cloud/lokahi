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

        return String.format(
                "%s %s/%s (%s/%s; %s) %s/%s (%s)",
                version,
                "Java",
                System.getProperty("java.version"),
                System.getProperty("java.vm.name"),
                System.getProperty("java.vm.version"),
                System.getProperty("java.vm.vendor"),
                System.getProperty("os.name").replaceAll("\\s+", "_"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"));
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
