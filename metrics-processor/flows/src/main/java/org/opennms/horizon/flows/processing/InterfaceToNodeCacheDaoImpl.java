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

package org.opennms.horizon.flows.processing;

import org.opennms.horizon.flows.dao.InterfaceToNodeCache;
import org.opennms.horizon.flows.grpc.client.InventoryClient;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.shared.utils.LocationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.opennms.horizon.shared.utils.InetAddressUtils.str;

/**
 * This class represents a singular instance that is used to map IP
 * addresses to known nodes.
 *
 * @author Seth
 * @author <a href="mailto:joed@opennms.org">Johan Edstrom</a>
 * @author <a href="mailto:weave@oculan.com">Brian Weaver </a>
 * @author <a href="mailto:tarus@opennms.org">Tarus Balog </a>
 * @author <a href="http://www.opennms.org/">OpenNMS </a>
 */
public class InterfaceToNodeCacheDaoImpl extends AbstractInterfaceToNodeCache implements InterfaceToNodeCache {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceToNodeCacheDaoImpl.class);

    private final InventoryClient client;

    private static class Key {
        private final String location;
        private final InetAddress ipAddress;
        private final String tenantId;

        public Key(String location, InetAddress ipAddress, String tenantId) {
            // Use the default location when location is null
            this.location = LocationUtils.getEffectiveLocationName(location);
            this.ipAddress = Objects.requireNonNull(ipAddress);
            this.tenantId = Objects.requireNonNull(tenantId);
        }

        public InetAddress getIpAddress() {
            return ipAddress;
        }

        public String getLocation() {
            return location;
        }

        public String getTenantId() {
            return tenantId;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            if (obj.getClass() != getClass()) {
                return false;
            }
            final Key that = (Key) obj;
            return Objects.equals(this.tenantId, that.tenantId)
                && Objects.equals(this.ipAddress, that.ipAddress)
                && Objects.equals(this.location, that.location);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.ipAddress, this.location);
        }

        @Override
        public String toString() {
            return String.format("Key[location='%s', ipAddress='%s']", this.location, this.ipAddress);
        }
    }

    private final ReadWriteLock m_lock = new ReentrantReadWriteLock();
    // change to use Map due to MultiMap will always return empty list, we want to differentiate null & empty list
    private Map<Key, Set<Entry>> m_managedAddresses = new HashMap<>();

    private final Timer refreshTimer = new Timer(getClass().getSimpleName());

    // in ms
    private final long refreshRate;

    public InterfaceToNodeCacheDaoImpl(InventoryClient client) {
        this(client, -1); // By default refreshing the cache is disabled
    }

    public InterfaceToNodeCacheDaoImpl(InventoryClient client, long refreshRate) {
        this.client = Objects.requireNonNull(client);
        this.refreshRate = refreshRate;
        if (refreshRate > 0) {
            refreshTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        m_lock.writeLock().lock();
                        m_managedAddresses.clear();
                        m_lock.writeLock().unlock();
                    } catch (Exception ex) {
                        LOG.error("An error occurred while synchronizing the datasource: {}", ex.getMessage(), ex);
                    }
                }
            }, refreshRate, refreshRate);
        }
    }

    @Override
    public Optional<Entry> getFirst(String location, InetAddress ipAddr, String tenantId) {
        if (ipAddr == null) {
            return Optional.empty();
        }
        m_lock.readLock().lock();
        try {
            var values = m_managedAddresses.get(new Key(location, ipAddr, tenantId));
            if (values != null) {
                if (values.isEmpty()) {
                    return Optional.empty();
                } else {
                    return values.stream().findFirst();
                }
            } else {
                m_lock.writeLock().lock();
                var iface = client.getIpInterfaceFromQuery(tenantId, str(ipAddr), location);
                var entries = new ArrayList<Entry>();
                if (iface != null) {
                    entries.add(convertToEntry(iface));
                }
                // keep put even empty list to differentiate between no record and not cache
                m_managedAddresses.put(new Key(location, ipAddr, tenantId), new HashSet<>(0));
                m_lock.readLock().unlock();
                return (values.isEmpty()) ? Optional.empty() : values.stream().findFirst();
            }
        } finally {
            m_lock.readLock().unlock();
        }
    }

    private Entry convertToEntry(IpInterfaceDTO iface) {
        return new Entry(iface.getNodeId(), iface.getId(), iface.getTenantId());
    }

    /**
     * Sets the IP Address and Node ID in the Map.
     *
     * @param addr   The IP Address to add.
     * @param nodeId The Node ID to add.
     * @return The nodeid if it existed in the map.
     */
    @Override
    public boolean setNodeId(final String location, final InetAddress addr, final long nodeId, final String tenantId) {
        if (addr == null || nodeId == -1) {
            return false;
        }

        IpInterfaceDTO iface = client.getIpInterfaceFromQuery(tenantId, str(addr), location);
        if (iface == null) {
            return false;
        }

        LOG.debug("setNodeId: adding IP address to cache: {}:{} -> {}", location, str(addr), nodeId);

        m_lock.writeLock().lock();
        try {
            var key = new Key(location, addr, tenantId);
            var entries = m_managedAddresses.get(key);
            if (entries == null) {
                entries = new HashSet<>();
                m_managedAddresses.put(key, entries);
            }
            return !entries.add(new Entry(nodeId, iface.getId(), tenantId));
        } finally {
            m_lock.writeLock().unlock();
        }
    }

    /**
     * Removes an address from the node ID map.
     *
     * @param address The address to remove from the node ID map.
     * @return The nodeid that was in the map.
     */
    @Override
    public boolean removeNodeId(final String location, final InetAddress address, final long nodeId, final String tenantId) {
        if (address == null) {
            LOG.warn("removeNodeId: null IP address");
            return false;
        }

        LOG.debug("removeNodeId: removing IP address from cache: {}:{}", location, str(address));

        m_lock.writeLock().lock();
        try {
            final Key key = new Key(location, address, tenantId);
            final var entries = m_managedAddresses.get(key);
            if (entries == null) {
                return false;
            }
            return entries.removeIf(e -> e.nodeId == nodeId);
        } finally {
            m_lock.writeLock().unlock();
        }
    }

    @Override
    public int size() {
        m_lock.readLock().lock();
        try {
            return m_managedAddresses.size();
        } finally {
            m_lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        m_lock.writeLock().lock();
        try {
            m_managedAddresses.clear();
        } finally {
            m_lock.writeLock().unlock();
        }
    }

    @Override
    public void removeInterfacesForNode(long nodeId) {
        m_lock.writeLock().lock();
        try {
            for (var entrySet : m_managedAddresses.entrySet()) {
                var matched = entrySet.getValue().stream().filter(e -> e.nodeId == nodeId);
                matched.forEach(m -> {
                    if (entrySet.getValue().remove(m)) {
                        LOG.debug("removeInterfacesForNode: removed interfaceId from cache: {}", m.interfaceId);
                    }
                });
            }
        } finally {
            m_lock.writeLock().unlock();
        }
    }
}
