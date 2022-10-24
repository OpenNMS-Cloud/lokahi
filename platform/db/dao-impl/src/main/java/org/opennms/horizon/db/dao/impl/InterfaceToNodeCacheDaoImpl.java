/*******************************************************************************
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
 *******************************************************************************/

package org.opennms.horizon.db.dao.impl;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.opennms.horizon.core.lib.LocationUtils;
import org.opennms.horizon.db.dao.api.AbstractInterfaceToNodeCache;
import org.opennms.horizon.db.dao.api.IpInterfaceDao;
import org.opennms.horizon.db.dao.api.NodeDao;
import org.opennms.horizon.db.dao.api.SessionUtils;
import org.opennms.horizon.db.model.OnmsIpInterface;
import org.opennms.horizon.db.model.OnmsNode;
import org.opennms.horizon.db.model.PrimaryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.Transactional;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static org.opennms.horizon.shared.utils.InetAddressUtils.str;


public class InterfaceToNodeCacheDaoImpl extends AbstractInterfaceToNodeCache {
    private static final Logger LOG = LoggerFactory.getLogger(InterfaceToNodeCacheDaoImpl.class);

    private final ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setNameFormat("sync-interface-to-node-cache")
        .build();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(threadFactory);
    private final CountDownLatch initialNodeSyncDone = new CountDownLatch(1);

    private static class Key {
        private final String location;
        private final InetAddress ipAddress;

        public Key(String location, InetAddress ipAddress) {
            // Use the default location when location is null
            this.location = LocationUtils.getEffectiveLocationName(location);
            this.ipAddress = Objects.requireNonNull(ipAddress);
        }

        public InetAddress getIpAddress() {
            return ipAddress;
        }

        public String getLocation() {
            return location;
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
            return Objects.equals(this.ipAddress, that.ipAddress)
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

    private static class Value implements Comparable<Value> {
        private final int nodeId;
        private final int interfaceId;
        private final PrimaryType type;


        private Value(final int nodeId,
                      final int interfaceId,
                      final PrimaryType type) {
            this.nodeId = nodeId;
            this.interfaceId = interfaceId;
            this.type = type;
        }

        public int getNodeId() {
            return this.nodeId;
        }

        public int getInterfaceId() {
            return this.interfaceId;
        }

        public PrimaryType getType() {
            return this.type;
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
            final Value that = (Value) obj;
            return Objects.equals(this.nodeId, that.nodeId)
                && Objects.equals(this.interfaceId, that.interfaceId)
                && Objects.equals(this.type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.nodeId, this.type.getCharCode());
        }

        @Override
        public String toString() {
            return String.format("Value[nodeId='%s', interfaceId='%s', type='%s']", this.nodeId, this.interfaceId, this.type);
        }

        @Override
        public int compareTo(final Value that) {
            return ComparisonChain.start()
                .compare(this.type, that.type)
                .compare(this.nodeId, that.nodeId)
                .compare(this.interfaceId, that.interfaceId)
                .result();
        }
    }

    private NodeDao nodeDao;

    private IpInterfaceDao ipInterfaceDao;

    private SessionUtils sessionUtils;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private SortedSetMultimap<Key, Value> managedAddresses = Multimaps.newSortedSetMultimap(Maps.newHashMap(), TreeSet::new);

    private final Timer refreshTimer = new Timer(getClass().getSimpleName());

    // in ms
    private final long refreshRate;

    public InterfaceToNodeCacheDaoImpl() {
        this(-1); // By default refreshing the cache is disabled
    }

    public InterfaceToNodeCacheDaoImpl(long refreshRate) {
        this.refreshRate = refreshRate;
    }

    public void init() {
        // sync datasource asynchronously in order to not block bean initialization.
        syncDataSourceAsynchronously().whenComplete((result, ex) -> {
            initialNodeSyncDone.countDown();
            if (refreshRate > 0) {
                refreshTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            dataSourceSync();
                        } catch (Exception ex) {
                            LOG.error("An error occurred while synchronizing the datasource: {}", ex.getMessage(), ex);
                        }
                    }
                }, refreshRate, refreshRate);
            }
        });
    }

    public void destroy() {
        initialNodeSyncDone.countDown();
        executorService.shutdownNow();
    }

    public NodeDao getNodeDao() {
        return nodeDao;
    }

    public void setNodeDao(NodeDao nodeDao) {
        this.nodeDao = nodeDao;
    }

    public IpInterfaceDao getIpInterfaceDao() {
        return ipInterfaceDao;
    }

    public void setIpInterfaceDao(IpInterfaceDao ipInterfaceDao) {
        this.ipInterfaceDao = ipInterfaceDao;
    }

    public SessionUtils getSessionUtils() {
        return sessionUtils;
    }

    public void setSessionUtils(SessionUtils sessionUtils) {
        this.sessionUtils = sessionUtils;
    }

    /**
     * Clears and synchronizes the internal known IP address cache with the
     * current information contained in the database. To synchronize the cache
     * the method opens a new connection to the database, loads the address,
     * and then closes it's connection.
     *
     * @throws java.sql.SQLException Thrown if the connection cannot be created or a database
     *                               error occurs.
     */
    @Override
    @Transactional
    public void dataSourceSync() {
        sessionUtils.withTransaction(() -> {
            dataSourceSyncWithinTransaction();
            return null;
        });

    }

    private CompletableFuture<Void> syncDataSourceAsynchronously() {
        return CompletableFuture.runAsync(this::dataSourceSync, executorService);
    }

    private void dataSourceSyncWithinTransaction() {
        /*
         * Make a new list with which we'll replace the existing one, that way
         * if something goes wrong with the DB we won't lose whatever was already
         * in there
         */
        final SortedSetMultimap<Key, Value> newAlreadyDiscovered = Multimaps.newSortedSetMultimap(Maps.newHashMap(), TreeSet::new);

        // Fetch all non-deleted nodes
        var builder = nodeDao.getEntityManager().getCriteriaBuilder();
        var criteriaQuery = builder.createQuery(OnmsNode.class);
        var root = criteriaQuery.from(OnmsNode.class);
        criteriaQuery.where(builder.notEqual(root.get("type"), String.valueOf(OnmsNode.NodeType.DELETED.value())));

        for (OnmsNode node : nodeDao.findMatching(criteriaQuery)) {
            for (final OnmsIpInterface iface : node.getIpInterfaces()) {
                // Skip deleted interfaces
                // TODO: Refactor the 'D' value with an enumeration
                if ("D".equals(iface.getIsManaged())) {
                    continue;
                }
                LOG.debug("Adding entry: {}:{} -> {}", node.getLocation().getLocationName(), iface.getIpAddress(), node.getId());
                newAlreadyDiscovered.put(new Key(node.getLocation().getLocationName(), iface.getIpAddress()), new Value(node.getId(), iface.getId(), iface.getIsSnmpPrimary()));
            }
        }

        try {
            lock.writeLock().lock();
            managedAddresses = newAlreadyDiscovered;
        } finally {
            lock.writeLock().unlock();
        }

        LOG.info("dataSourceSync: initialized list of managed IP addresses with {} members", managedAddresses.size());
    }

    @Override
    public Optional<Entry> getFirst(String location, InetAddress ipAddr) {
        if (ipAddr == null) {
            return Optional.empty();
        }
        waitForInitialNodeSync();
        lock.readLock().lock();
        try {
            var values = managedAddresses.get(new Key(location, ipAddr));
            return values.isEmpty() ? Optional.empty() : Optional.of(new Entry(values.first().nodeId, values.first().interfaceId));
        } finally {
            lock.readLock().unlock();
        }
    }

    private void waitForInitialNodeSync() {
        try {
            initialNodeSyncDone.await();
        } catch (InterruptedException e) {
            LOG.warn("Wait for node cache sync interrupted", e);
        }
    }

    /**
     * Sets the IP Address and Node ID in the Map.
     *
     * @param addr   The IP Address to add.
     * @param nodeid The Node ID to add.
     * @return The nodeid if it existed in the map.
     */
    @Override
    @Transactional
    public boolean setNodeId(final String location, final InetAddress addr, final int nodeid) {
        if (addr == null || nodeid == -1) {
            return false;
        }

        final OnmsIpInterface iface = ipInterfaceDao.findByNodeIdAndIpAddress(nodeid, str(addr));
        if (iface == null) {
            return false;
        }

        LOG.debug("setNodeId: adding IP address to cache: {}:{} -> {}", location, str(addr), nodeid);

        lock.writeLock().lock();
        try {
            return managedAddresses.put(new Key(location, addr), new Value(nodeid, iface.getId(), iface.getIsSnmpPrimary()));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes an address from the node ID map.
     *
     * @param address The address to remove from the node ID map.
     * @return The nodeid that was in the map.
     */
    @Override
    public boolean removeNodeId(final String location, final InetAddress address, final int nodeId) {
        if (address == null) {
            LOG.warn("removeNodeId: null IP address");
            return false;
        }

        LOG.debug("removeNodeId: removing IP address from cache: {}:{}", location, str(address));

        lock.writeLock().lock();
        try {
            final Key key = new Key(location, address);
            return managedAddresses.get(key).removeIf(e -> e.nodeId == nodeId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int size() {
        waitForInitialNodeSync();
        lock.readLock().lock();
        try {
            return managedAddresses.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            managedAddresses.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void removeInterfacesForNode(int nodeId) {
        lock.writeLock().lock();
        try {
            List<Map.Entry<Key, Value>> keyValues = managedAddresses.entries().stream()
                .filter(keyValueEntry -> keyValueEntry.getValue().getNodeId() == nodeId)
                .collect(Collectors.toList());
            keyValues.forEach(keyValue -> {
                boolean succeeded = managedAddresses.remove(keyValue.getKey(), keyValue.getValue());
                if (succeeded) {
                    LOG.debug("removeInterfacesForNode: removed IP address from cache: {}", str(keyValue.getKey().getIpAddress()));
                }
            });
        } finally {
            lock.writeLock().unlock();
        }
    }
}

