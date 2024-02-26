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
package org.opennms.miniongateway.grpc.server.model;

import java.io.Serializable;
import java.util.Objects;

public class TenantKey implements Serializable {
    private final String tenantId;
    private final String key;

    public TenantKey(String tenantId, String key) {
        this.tenantId = tenantId;
        this.key = key;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getKey() {
        return key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TenantKey tenantKey = (TenantKey) o;
        return Objects.equals(tenantId, tenantKey.tenantId) && Objects.equals(key, tenantKey.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, key);
    }

    @Override
    public String toString() {
        return "TenantKey{" + "tenantId='" + tenantId + '\'' + ", key='" + key + '\'' + '}';
    }
}
