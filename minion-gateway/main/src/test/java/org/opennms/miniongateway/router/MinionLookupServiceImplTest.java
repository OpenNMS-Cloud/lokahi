package org.opennms.miniongateway.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCluster;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.opennms.horizon.shared.ipc.grpc.server.manager.MinionInfo;
import org.opennms.miniongateway.grpc.server.model.TenantKey;

public class MinionLookupServiceImplTest {

    @Mock
    private Ignite ignite;

    @Mock
    private IgniteCluster igniteCluster;

    @Mock
    private ClusterNode clusterNode;

    @Mock
    IgniteCache igniteLocationCache;

    @Mock
    IgniteCache igniteIdCache;

    @Mock
    Lock lock;

    private Map<TenantKey, Set<UUID>> locationMap = new HashMap<>();

    private Map<TenantKey, UUID> idMap = new HashMap<>();

    MinionLookupService minionLookupService;

    UUID localNodeUUID;

    MinionInfo minionInfo;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        localNodeUUID = UUID.randomUUID();
        minionInfo = new MinionInfo();
        minionInfo.setId("blahId");
        minionInfo.setLocation("blahLocation");

        when(igniteLocationCache.lock(any())).thenReturn(lock);
        when(igniteIdCache.get(any())).thenAnswer((Answer<UUID>) invocationOnMock -> idMap.get(invocationOnMock.getArgument(0)));
        when(igniteIdCache.remove(any())).thenAnswer((Answer<Boolean>) invocationOnMock -> ( idMap.remove(invocationOnMock.getArgument(0)) != null ));
        doAnswer((Answer<UUID>) invocationOnMock -> idMap.put(invocationOnMock.getArgument(0), invocationOnMock.getArgument(1))).
            when(igniteIdCache).put(any(), any());

        when(igniteLocationCache.get(any())).thenAnswer((Answer<Set<UUID>>) invocationOnMock -> locationMap.get(invocationOnMock.getArgument(0)));
        when(igniteLocationCache.remove(any())).thenAnswer((Answer<Boolean>) invocationOnMock -> ( locationMap.remove(invocationOnMock.getArgument(0)) != null));
        doAnswer((Answer<Set<UUID>>) invocationOnMock -> locationMap.put(invocationOnMock.getArgument(0), invocationOnMock.getArgument(1))).
            when(igniteLocationCache).put(any(), any());

        doReturn(igniteIdCache).when(ignite).getOrCreateCache(argThat((CacheConfiguration config) -> config.getName().equals(MinionLookupServiceImpl.MINIONS_BY_ID)));
        doReturn(igniteLocationCache).when(ignite).getOrCreateCache(argThat((CacheConfiguration config) -> config.getName().equals(MinionLookupServiceImpl.MINIONS_BY_LOCATION)));

        when(ignite.cache(eq(MinionLookupServiceImpl.MINIONS_BY_ID))).thenReturn(igniteIdCache);
        when(ignite.cache(eq(MinionLookupServiceImpl.MINIONS_BY_LOCATION))).thenReturn(igniteLocationCache);
        when(ignite.cluster()).thenReturn(igniteCluster);
        when(igniteCluster.localNode()).thenReturn(clusterNode);
        when(clusterNode.id()).thenReturn(localNodeUUID);


        minionLookupService = new MinionLookupServiceImpl(ignite);
    }
    

    @Test
    public void findGatewayNodeWithId() {
        generateMinions(3);

        UUID uuid = minionLookupService.findGatewayNodeWithId("tenant", "minion1");
        assertEquals(localNodeUUID, uuid);

        uuid = minionLookupService.findGatewayNodeWithId("tenant", "minion2");
        assertEquals(localNodeUUID, uuid);

        uuid = minionLookupService.findGatewayNodeWithId("tenant", "bogus");
        assertNull(uuid);
    }

    @Test
    public void findGatewayNodeWithLocation() {
        generateMinions(3);

        List<UUID> uuids = minionLookupService.findGatewayNodeWithLocation("tenant", "location");
        assertNotNull(uuids);
        assertEquals(1, uuids.size());

        assertEquals(localNodeUUID, uuids.stream().findFirst().get());

        uuids = minionLookupService.findGatewayNodeWithLocation("tenant", "badLocation");
        assertNull(uuids);

    }

    @Test
    public void onMinionRemoved() {
        MinionInfo minionInfo1 = new MinionInfo();
        minionInfo1.setTenantId("tenant");
        minionInfo1.setId("minion");
        minionInfo1.setLocation(("location"));

        minionLookupService.onMinionAdded(1, minionInfo1);

        assertNotNull(minionLookupService.findGatewayNodeWithId("tenant", minionInfo1.getId()));

        minionLookupService.onMinionRemoved(1, minionInfo1);

        assertNull(minionLookupService.findGatewayNodeWithId("tenant", minionInfo1.getId()));
    }

    private void generateMinions(int num) {
        for (int i=0;i<num;i++) {
            MinionInfo minionInfo1 = new MinionInfo();
            minionInfo1.setTenantId("tenant");
            minionInfo1.setId("minion"+i);
            minionInfo1.setLocation(("location"));

            minionLookupService.onMinionAdded(i, minionInfo1);
        }
    }
}
