/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2003-2023 The OpenNMS Group, Inc.
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
 *******************************************************************************/

package org.opennms.horizon.flows.dao;

import java.net.InetAddress;
import java.util.Objects;
import java.util.Optional;

public interface InterfaceToNodeCache {

	boolean setNodeId(String location, InetAddress ipAddr, long nodeId, String tenantId);

	boolean removeNodeId(String location, InetAddress ipAddr, long nodeId, String tenantId);

	int size();

	/**
	 * Should only be used for testing.
	 */
	void clear();

	Optional<Entry> getFirst(String location, InetAddress ipAddr, String tenantId);

	default Optional<Long> getFirstNodeId(String location, InetAddress ipAddr, String tenantId) {
		return this.getFirst(location, ipAddr, tenantId).map(e -> e.nodeId);
	}

	void removeInterfacesForNode(long nodeId);

	class Entry {
		public final long nodeId;
		public final long interfaceId;
		public final String tenantId;

		public Entry(final long nodeId, final long interfaceId, final String tenantId) {
			this.nodeId = nodeId;
			this.interfaceId = interfaceId;
			this.tenantId = tenantId;
		}

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}

			if (!(o instanceof Entry)) {
				return false;

			}
			final Entry entry = (Entry) o;
			return this.nodeId == entry.nodeId &&
				   this.interfaceId == entry.interfaceId &&
                   this.tenantId.equals(entry.tenantId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.nodeId,
			                    this.interfaceId);
		}
	}
}
