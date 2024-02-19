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

import io.grpc.ManagedChannel;
import lombok.Setter;

public class SecuritySwitchedChannelFactory implements ManagedChannelFactory {

    private final ManagedChannelFactory plainText;
    private final ManagedChannelFactory secured;

    @Setter
    private boolean tlsEnabled;

    public SecuritySwitchedChannelFactory(ManagedChannelFactory plainText, ManagedChannelFactory secured) {
        this.plainText = plainText;
        this.secured = secured;
    }

    @Override
    public ManagedChannel create(String hostname, int port, String authority) {
        if (tlsEnabled) {
            return secured.create(hostname, port, authority);
        }
        return plainText.create(hostname, port, authority);
    }
}
