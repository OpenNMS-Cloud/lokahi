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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.ChannelCredentials;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlainTextChannelFactoryTest {

    @Mock
    protected ChannelBuilderFactory channelBuilderFactory;

    @Mock
    protected ManagedChannelBuilder managedChannelBuilder;

    @Test
    public void verifyPlainTextChannelBuilder() {
        prepareMocks("foo", 8990, null, null);

        new PlainTextChannelFactory(channelBuilderFactory).create("foo", 8990, null);

        verifyMock("foo", 8990, null, null);
    }

    @Test
    public void verifyPlainTextChannelBuilderWithCustomAuthority() {
        prepareMocks("faz", 8990, "bar", null);

        new PlainTextChannelFactory(channelBuilderFactory).create("faz", 8990, "bar");

        verifyMock("faz", 8990, "bar", null);
    }

    protected void prepareMocks(String host, int port, String authority, ChannelCredentials credentials) {
        when(channelBuilderFactory.create(host, port, authority, credentials)).thenReturn(managedChannelBuilder);

        when(managedChannelBuilder.usePlaintext()).thenReturn(managedChannelBuilder);
    }

    protected void verifyMock(String host, int port, String authority, ChannelCredentials credentials) {
        verify(channelBuilderFactory).create(host, port, authority, credentials);
    }
}
