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
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Fairly basic verification of channel construction logic based on tls switch.
 */
@ExtendWith(MockitoExtension.class)
public class SecuritySwitchedChannelFactoryTest {

    @Mock(name = "plainText")
    ManagedChannelFactory plainText;

    @Mock(name = "secure")
    ManagedChannelFactory secure;

    SecuritySwitchedChannelFactory channelFactory;

    @BeforeEach
    public void setUp() {
        channelFactory = new SecuritySwitchedChannelFactory(plainText, secure);
    }

    @Test
    public void testTlsFactory() {
        channelFactory.setTlsEnabled(true);
        channelFactory.create("foo", 123, null);
        channelFactory.create("bar", 321, "foo");

        verify(secure).create("foo", 123, null);
        verify(secure).create("bar", 321, "foo");
        verifyNoInteractions(plainText);
    }

    @Test
    public void testPlainTextFactory() {
        channelFactory.setTlsEnabled(false);
        channelFactory.create("foo", 123, null);
        channelFactory.create("bar", 321, "foo");

        verify(plainText).create("foo", 123, null);
        verify(plainText).create("bar", 321, "foo");
        verifyNoInteractions(secure);
    }
}
