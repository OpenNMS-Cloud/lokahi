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
package org.opennms.miniongateway.router;

import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionInfo;
import org.opennms.miniongateway.grpc.server.model.TenantKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinionLookupServiceImpl implements MinionLookupService {

    public static final String MINIONS_BY_ID = "minionsById";
    public static final String MINIONS_BY_LOCATION = "minionsByLocation";

    private final Logger logger = LoggerFactory.getLogger(MinionLookupServiceImpl.class);

    private Ignite ignite;

    private IgniteCache<TenantKey, UUID> minionByIdCache;
    private IgniteCache<TenantKey, Queue<UUID>> minionByLocationCache;

    public MinionLookupServiceImpl(Ignite ignite) {
        logger.info("############ MINION ROUTER SERVICE INITIALIZED");

        this.ignite = ignite;

        // We need to be able to lock the caches when inserting new values, to insure that there is no race condition
        // with competing threads that may be trying to insert the same new location. So we will configure both caches
        // to be TRANSACTIONAL and be ready for locking.
        minionByIdCache = ignite.cache(MINIONS_BY_ID);
        minionByLocationCache = ignite.cache(MINIONS_BY_LOCATION); // getOrCreateCache(minionByLocationCacheConfig);
    }

    @Override
    public UUID findGatewayNodeWithId(String tenantId, String id) {
        return minionByIdCache.get(new TenantKey(tenantId, id));
    }

    @Override
    public List<UUID> findGatewayNodeWithLocation(String tenantId, String location) {
        // TODO consider different structure to retain node identifiers to avoid wrapping into list
        // result must be indexed to support balancing of requests sent onto location (see Routing Task)
        Queue<UUID> uuids = minionByLocationCache.get(new TenantKey(tenantId, location));
        return uuids != null ? List.copyOf(uuids) : null;
    }

    @Override
    public void onMinionAdded(long sequence, MinionInfo minionInfo) {

        UUID localUUID = ignite.cluster().localNode().id();

        minionByIdCache.put(new TenantKey(minionInfo.getTenantId(), minionInfo.getId()), localUUID);

        Lock lock = minionByLocationCache.lock(new TenantKey(minionInfo.getTenantId(), minionInfo.getLocation()));
        try {
            lock.lock();

            Queue<UUID> existingMinions =
                    minionByLocationCache.get(new TenantKey(minionInfo.getTenantId(), minionInfo.getLocation()));
            if (existingMinions == null) {
                existingMinions = new ConcurrentLinkedQueue<>();
                minionByLocationCache.put(
                        new TenantKey(minionInfo.getTenantId(), minionInfo.getLocation()), existingMinions);
            }
            // TODO: for now, seems we can modify in place and not have to put this back in.
            existingMinions.add(localUUID);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void onMinionRemoved(long sequence, MinionInfo minionInfo) {

        UUID localUUID = ignite.cluster().localNode().id();

        minionByIdCache.remove(new TenantKey(minionInfo.getTenantId(), minionInfo.getId()));

        Queue<UUID> existingMinions =
                minionByLocationCache.get(new TenantKey(minionInfo.getTenantId(), minionInfo.getLocation()));
        if (existingMinions != null) {
            existingMinions.remove(localUUID);
            if (existingMinions.isEmpty()) {
                minionByLocationCache.remove(new TenantKey(minionInfo.getTenantId(), minionInfo.getLocation()));
            }
        }
    }
}
