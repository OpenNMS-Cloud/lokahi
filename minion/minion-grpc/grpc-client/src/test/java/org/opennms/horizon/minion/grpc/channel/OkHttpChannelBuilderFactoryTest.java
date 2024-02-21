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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.grpc.ChannelCredentials;
import io.grpc.ManagedChannelBuilder;
import io.grpc.TlsChannelCredentials;
import org.junit.jupiter.api.Test;

class OkHttpChannelBuilderFactoryTest {

    @Test
    public void verifyPlainText() {
        OkHttpChannelBuilderFactory channelBuilderFactory = new OkHttpChannelBuilderFactory();
        channelBuilderFactory.create("host", 443, null, null);
    }

    @Test
    public void verifyTls() {
        ChannelCredentials credentials = TlsChannelCredentials.newBuilder().build();
        OkHttpChannelBuilderFactory channelBuilderFactory = new OkHttpChannelBuilderFactory();
        channelBuilderFactory.create("host", 443, null, credentials);
    }

    @Test
    public void verifyMessageSize() {
        OkHttpChannelBuilderFactory channelBuilderFactory = new OkHttpChannelBuilderFactory();
        channelBuilderFactory.setMaxInboundMessageSize(1000);
        ManagedChannelBuilder<?> channelBuilder = channelBuilderFactory.create("host", 443, "", null);

        // no way to verify it
    }

    @Test
    public void verifyOverrideAuthority() {
        OkHttpChannelBuilderFactory channelBuilderFactory = new OkHttpChannelBuilderFactory();
        channelBuilderFactory.setMaxInboundMessageSize(1000);
        ManagedChannelBuilder<?> channelBuilder = channelBuilderFactory.create("host", 443, "desired", null);

        // no way to verify it
    }

    @Test
    public void verifyUserAgentNoVersion() {
        OkHttpChannelBuilderFactory channelBuilderFactory = new OkHttpChannelBuilderFactory();
        // no version set
        channelBuilderFactory.setGitBranch("legislative");
        channelBuilderFactory.setGitDescribe("excellent");
        channelBuilderFactory.setImageCreated("1989-02-17");

        String userAgent = channelBuilderFactory.getUserAgent();
        assertTrue(userAgent.contains("/excellent "));
        assertTrue(userAgent.contains("(built 1989-02-17;"));
        assertTrue(userAgent.contains("; branch legislative"));
    }

    @Test
    public void verifyUserAgentVersion() {
        OkHttpChannelBuilderFactory channelBuilderFactory = new OkHttpChannelBuilderFactory();
        channelBuilderFactory.setImageVersion("2");
        channelBuilderFactory.setGitBranch("judicial");
        channelBuilderFactory.setGitDescribe("bogus"); // won't show up in output
        channelBuilderFactory.setImageCreated("1991-07-19"); // won't show up in output

        String userAgent = channelBuilderFactory.getUserAgent();
        assertTrue(userAgent.contains("/2 "));
        assertTrue(userAgent.contains("bogus"));
        assertFalse(userAgent.contains("judicial"));
        assertFalse(userAgent.contains("1991-07-19"));
    }
}
