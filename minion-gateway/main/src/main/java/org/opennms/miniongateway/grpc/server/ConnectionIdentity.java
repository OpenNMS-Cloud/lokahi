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
package org.opennms.miniongateway.grpc.server;

import java.util.Objects;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.horizon.shared.ipc.rpc.IpcIdentity;

public class ConnectionIdentity implements IpcIdentity {

    private final String systemId;

    public ConnectionIdentity(Identity identity) {
        this(identity.getSystemId());
    }

    public ConnectionIdentity(String systemId) {
        this.systemId = systemId;
    }

    @Override
    public String getId() {
        return systemId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ConnectionIdentity that)) {
            return false;
        }
        return Objects.equals(systemId, that.systemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(systemId);
    }
}
