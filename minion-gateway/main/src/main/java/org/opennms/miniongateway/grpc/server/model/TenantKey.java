/*
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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
        return Objects.equals(tenantId, tenantKey.tenantId) &&
            Objects.equals(key, tenantKey.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, key);
    }

    @Override
    public String toString() {
        return "TenantKey{" +
            "tenantId='" + tenantId + '\'' +
            ", key='" + key + '\'' +
            '}';
    }
}
