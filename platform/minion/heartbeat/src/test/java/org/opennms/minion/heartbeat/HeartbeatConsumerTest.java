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

package org.opennms.minion.heartbeat;

import io.prometheus.client.CollectorRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.opennms.horizon.core.identity.IdentityImpl;
import org.opennms.horizon.db.dao.api.MinionDao;
import org.opennms.horizon.db.dao.api.SessionUtils;
import org.opennms.horizon.db.model.OnmsMinion;
import org.opennms.horizon.events.api.EventForwarder;
import org.opennms.horizon.ipc.sink.api.MessageConsumerManager;
import org.opennms.horizon.metrics.api.OnmsMetricsAdapter;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.mockito.Mockito.mock;

public class HeartbeatConsumerTest {

    @Test
    public void testHeartbeatConsumer() {
        MinionDao minionDao = new MockMinionDao();
        SessionUtils sessionUtils = new MockSessionUtils();
        MessageConsumerManager messageConsumerManager = mock(MessageConsumerManager.class);
        MockMetricsAdapter onmsMetricsAdapter = new MockMetricsAdapter();
        EventForwarder eventForwarder = mock(EventForwarder.class);

        HeartbeatConsumer heartbeatConsumer = new HeartbeatConsumer(messageConsumerManager, minionDao,
            sessionUtils, onmsMetricsAdapter, eventForwarder);

        // Case 1 : No Minion exists in DB yet
        String minionId = "minion-01";
        String location = "minion";
        MinionIdentityDTO minionIdentityDTO = new MinionIdentityDTO(new IdentityImpl(minionId, "minion", "MINION"));
        Instant currentTime = Instant.now();
        Date currentDate = Date.from(currentTime);
        minionIdentityDTO.setTimestamp(currentDate);
        // Send an update to Heartbeat Consumer
        heartbeatConsumer.handleMessage(minionIdentityDTO);
        OnmsMinion minion = minionDao.findById(minionId);
        // Minion last updated should match what's passed on with the message.
        Assert.assertEquals(minion.getLastUpdated(), currentDate);
        String[] labelNames = {"instance", "location"};
        String[] labelValues = {minionId, location};
        Double upTime = onmsMetricsAdapter.getCollectorRegistry()
            .getSampleValue("minion_uptime_sec", labelNames, labelValues);
        Assert.assertEquals(0, upTime, .001);


        // Case 2: Now that minion exists, send an update with latest timestamp that is 30 secs after currentTime.
        int heartBeatDelta = 30;
        Instant afterTime = currentTime.plusSeconds(heartBeatDelta);
        minionIdentityDTO.setTimestamp(Date.from(afterTime));
        // Send an update to Heartbeat Consumer
        heartbeatConsumer.handleMessage(minionIdentityDTO);
        minion = minionDao.findById(minionId);
        // Minion last updated should match what's passed on with the message.
        Assert.assertEquals(minion.getLastUpdated(), Date.from(afterTime));
        Double updatedUpTime = onmsMetricsAdapter.getCollectorRegistry()
            .getSampleValue("minion_uptime_sec", labelNames, labelValues);
        Assert.assertEquals(heartBeatDelta, updatedUpTime, .001);

        // Case 3: send an update with obsolete timestamp that is 30 secs before than currentTime
        Instant beforeTime = currentTime.minusSeconds(30);
        minionIdentityDTO.setTimestamp(Date.from(beforeTime));
        // Send an update to Heartbeat Consumer
        heartbeatConsumer.handleMessage(minionIdentityDTO);
        minion = minionDao.findById(minionId);
        // Minion last updated shouldn't match what's passed on with the message.
        Assert.assertNotEquals(minion.getLastUpdated(), Date.from(beforeTime));
        // No update on upTime as we send an outdated entry
        Double upTimeAfterOutDatedHeartbeat = onmsMetricsAdapter.getCollectorRegistry()
            .getSampleValue("minion_uptime_sec", labelNames, labelValues);
        Assert.assertEquals(heartBeatDelta, upTimeAfterOutDatedHeartbeat, .001);
    }


    public static class MockSessionUtils implements SessionUtils {

        @Override
        public <V> V withTransaction(Supplier<V> supplier) {
            return supplier.get();
        }

        @Override
        public <V> V withReadOnlyTransaction(Supplier<V> supplier) {
            return supplier.get();
        }

        @Override
        public void withTransaction(Runnable runnable) {
            runnable.run();
        }

    }

    public static class MockMinionDao implements MinionDao {

        private Map<String, OnmsMinion> minions = new HashMap<>();

        @Override
        public OnmsMinion findById(final String id) {
            return minions.get(id);
        }

        @Override
        public EntityManager getEntityManager() {
            return null;
        }

        @Override
        public void delete(OnmsMinion entity) {

        }

        @Override
        public void delete(String key) {

        }

        @Override
        public List<OnmsMinion> findAll() {
            return List.copyOf(minions.values());
        }

        @Override
        public OnmsMinion get(String id) {
            return null;
        }

        @Override
        public String save(OnmsMinion entity) {
            return null;
        }

        @Override
        public void saveOrUpdate(OnmsMinion minion) {
            minions.put(minion.getId(), minion);
        }

        @Override
        public void update(OnmsMinion entity) {

        }

        @Override
        public void flush() {

        }

        @Override
        public List<OnmsMinion> findMatching(CriteriaQuery<?> query) {
            return null;
        }

        @Override
        public long countAll() {
            return 0;
        }

    }

    public static class MockMetricsAdapter implements OnmsMetricsAdapter {

        private CollectorRegistry collectorRegistry;

        @Override
        public void pushMetrics(CollectorRegistry registry, Map<String, String> groupingKey) {
            collectorRegistry = registry;
        }

        public CollectorRegistry getCollectorRegistry() {
            return collectorRegistry;
        }
    }

}
