package org.opennms.miniongateway.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorResult;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCluster;
import org.apache.ignite.cache.CacheEntry;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.cache.ReadRepairStrategy;
import org.apache.ignite.cache.query.FieldsQueryCursor;
import org.apache.ignite.cache.query.Query;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.QueryDetailMetrics;
import org.apache.ignite.cache.query.QueryMetrics;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.cluster.ClusterGroup;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.lang.IgniteBiPredicate;
import org.apache.ignite.lang.IgniteClosure;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.mxbean.CacheMetricsMXBean;
import org.apache.ignite.transactions.TransactionException;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opennms.core.ipc.grpc.server.manager.MinionInfo;
import org.opennms.horizon.shared.ignite.remoteasync.MinionLookupService;

public class MinionLookupServiceImplTest {

    @Mock
    private Ignite ignite;

    @Mock
    private IgniteCluster igniteCluster;

    @Mock
    private ClusterNode clusterNode;
    
    IgniteCache igniteLocationCache;

    IgniteCache igniteIdCache;

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
        igniteIdCache = new myIgniteCache<String, String>();
        igniteLocationCache = new myIgniteCache<String, Queue<String>>();
        when(ignite.getOrCreateCache(eq(MinionLookupServiceImpl.MINIONS_BY_ID))).thenReturn(igniteIdCache);
        when(ignite.getOrCreateCache(eq(MinionLookupServiceImpl.MINIONS_BY_LOCATION))).thenReturn(igniteLocationCache);
        when(ignite.cache(eq(MinionLookupServiceImpl.MINIONS_BY_ID))).thenReturn(igniteIdCache);
        when(ignite.cache(eq(MinionLookupServiceImpl.MINIONS_BY_ID))).thenReturn(igniteLocationCache);
        when(ignite.cluster()).thenReturn(igniteCluster);
        when(igniteCluster.localNode()).thenReturn(clusterNode);
        when(clusterNode.id()).thenReturn(localNodeUUID);

        
        minionLookupService = new MinionLookupServiceImpl(ignite);
    }
    

    @Test
    public void findGatewayNodeWithId() {
        generateMinions(3);

        UUID uuid = minionLookupService.findGatewayNodeWithId("minion1");
        assertEquals(localNodeUUID, uuid);

        uuid = minionLookupService.findGatewayNodeWithId("minion2");
        assertEquals(localNodeUUID, uuid);

        uuid = minionLookupService.findGatewayNodeWithId("bogus");
        assertNull(uuid);
    }

    @Test
    public void findGatewayNodeWithLocation() {
        generateMinions(3);

        Queue<UUID> uuids = minionLookupService.findGatewayNodeWithLocation("location");
        assertNotNull(uuids);
        assertEquals(3, uuids.size());

        assertEquals(localNodeUUID, uuids.stream().findFirst().get());

        uuids = minionLookupService.findGatewayNodeWithLocation("badLocation");
        assertNull(uuids);

    }

    @Test
    public void onMinionRemoved() {
        MinionInfo minionInfo1 = new MinionInfo();
        minionInfo1.setId("minion");
        minionInfo1.setLocation(("location"));

        minionLookupService.onMinionAdded(1, minionInfo1);

        assertNotNull(minionLookupService.findGatewayNodeWithId(minionInfo1.getId()));

        minionLookupService.onMinionRemoved(1, minionInfo1);

        assertNull(minionLookupService.findGatewayNodeWithId(minionInfo1.getId()));
    }

    private void generateMinions(int num) {
        for (int i=0;i<num;i++) {
            MinionInfo minionInfo1 = new MinionInfo();
            minionInfo1.setId("minion"+i);
            minionInfo1.setLocation(("location"));

            minionLookupService.onMinionAdded(i, minionInfo1);
        }
    }

    private class myIgniteCache<K, V> implements IgniteCache<K, V> {

        private Map<K, V> internalMap = new HashMap<>();

        @Override
        public IgniteCache<K, V> withAsync() {
            return null;
        }

        @Override
        public boolean isAsync() {
            return false;
        }

        @Override
        public <R> IgniteFuture<R> future() {
            return null;
        }

        @Override
        public <C extends Configuration<K, V>> C getConfiguration(Class<C> clazz) {
            return null;
        }

        @Override
        public IgniteCache<K, V> withExpiryPolicy(ExpiryPolicy plc) {
            return null;
        }

        @Override
        public IgniteCache<K, V> withSkipStore() {
            return null;
        }

        @Override
        public IgniteCache<K, V> withNoRetries() {
            return null;
        }

        @Override
        public IgniteCache<K, V> withPartitionRecover() {
            return null;
        }

        @Override
        public IgniteCache<K, V> withReadRepair(ReadRepairStrategy strategy) {
            return null;
        }

        @Override
        public <K1, V1> IgniteCache<K1, V1> withKeepBinary() {
            return null;
        }

        @Override
        public <K1, V1> IgniteCache<K1, V1> withAllowAtomicOpsInTx() {
            return null;
        }

        @Override
        public void loadCache(@Nullable IgniteBiPredicate<K, V> p, @Nullable Object... args) throws CacheException {

        }

        @Override
        public IgniteFuture<Void> loadCacheAsync(@Nullable IgniteBiPredicate<K, V> p, @Nullable Object... args)
            throws CacheException {
            return null;
        }

        @Override
        public void localLoadCache(@Nullable IgniteBiPredicate<K, V> p, @Nullable Object... args)
            throws CacheException {

        }

        @Override
        public IgniteFuture<Void> localLoadCacheAsync(@Nullable IgniteBiPredicate<K, V> p, @Nullable Object... args)
            throws CacheException {
            return null;
        }

        @Override
        public V getAndPutIfAbsent(K key, V val) throws CacheException, TransactionException {
            return null;
        }

        @Override
        public IgniteFuture<V> getAndPutIfAbsentAsync(K key, V val) throws CacheException, TransactionException {
            return null;
        }

        @Override
        public Lock lock(K key) {
            return null;
        }

        @Override
        public Lock lockAll(Collection<? extends K> keys) {
            return null;
        }

        @Override
        public boolean isLocalLocked(K key, boolean byCurrThread) {
            return false;
        }

        @Override
        public <R> QueryCursor<R> query(Query<R> qry) {
            return null;
        }

        @Override
        public FieldsQueryCursor<List<?>> query(SqlFieldsQuery qry) {
            return null;
        }

        @Override
        public <T, R> QueryCursor<R> query(Query<T> qry, IgniteClosure<T, R> transformer) {
            return null;
        }

        @Override
        public Iterable<Entry<K, V>> localEntries(CachePeekMode... peekModes) throws CacheException {
            return null;
        }

        @Override
        public QueryMetrics queryMetrics() {
            return null;
        }

        @Override
        public void resetQueryMetrics() {

        }

        @Override
        public Collection<? extends QueryDetailMetrics> queryDetailMetrics() {
            return null;
        }

        @Override
        public void resetQueryDetailMetrics() {

        }

        @Override
        public void localEvict(Collection<? extends K> keys) {

        }

        @Override
        public V localPeek(K key, CachePeekMode... peekModes) {
            return null;
        }

        @Override
        public int size(CachePeekMode... peekModes) throws CacheException {
            return 0;
        }

        @Override
        public IgniteFuture<Integer> sizeAsync(CachePeekMode... peekModes) throws CacheException {
            return null;
        }

        @Override
        public long sizeLong(CachePeekMode... peekModes) throws CacheException {
            return 0;
        }

        @Override
        public IgniteFuture<Long> sizeLongAsync(CachePeekMode... peekModes) throws CacheException {
            return null;
        }

        @Override
        public long sizeLong(int partition, CachePeekMode... peekModes) throws CacheException {
            return 0;
        }

        @Override
        public IgniteFuture<Long> sizeLongAsync(int partition, CachePeekMode... peekModes) throws CacheException {
            return null;
        }

        @Override
        public int localSize(CachePeekMode... peekModes) {
            return 0;
        }

        @Override
        public long localSizeLong(CachePeekMode... peekModes) {
            return 0;
        }

        @Override
        public long localSizeLong(int partition, CachePeekMode... peekModes) {
            return 0;
        }

        @Override
        public <T> Map<K, EntryProcessorResult<T>> invokeAll(Map<? extends K, ? extends EntryProcessor<K, V, T>> map,
            Object... args) throws TransactionException {
            return null;
        }

        @Override
        public <T> IgniteFuture<Map<K, EntryProcessorResult<T>>> invokeAllAsync(
            Map<? extends K, ? extends EntryProcessor<K, V, T>> map, Object... args) throws TransactionException {
            return null;
        }

        @Override
        public V get(K key) throws TransactionException {
            return internalMap.get(key);
        }

        @Override
        public IgniteFuture<V> getAsync(K key) {
            return null;
        }

        @Override
        public CacheEntry<K, V> getEntry(K key) throws TransactionException {
            return null;
        }

        @Override
        public IgniteFuture<CacheEntry<K, V>> getEntryAsync(K key) throws TransactionException {
            return null;
        }

        @Override
        public Map<K, V> getAll(Set<? extends K> keys) throws TransactionException {
            return null;
        }

        @Override
        public IgniteFuture<Map<K, V>> getAllAsync(Set<? extends K> keys) throws TransactionException {
            return null;
        }

        @Override
        public Collection<CacheEntry<K, V>> getEntries(Set<? extends K> keys) throws TransactionException {
            return null;
        }

        @Override
        public IgniteFuture<Collection<CacheEntry<K, V>>> getEntriesAsync(Set<? extends K> keys)
            throws TransactionException {
            return null;
        }

        @Override
        public Map<K, V> getAllOutTx(Set<? extends K> keys) {
            return null;
        }

        @Override
        public IgniteFuture<Map<K, V>> getAllOutTxAsync(Set<? extends K> keys) {
            return null;
        }

        @Override
        public boolean containsKey(K key) throws TransactionException {
            return false;
        }

        @Override
        public void loadAll(Set<? extends K> set, boolean b, CompletionListener completionListener) {

        }

        @Override
        public IgniteFuture<Boolean> containsKeyAsync(K key) throws TransactionException {
            return null;
        }

        @Override
        public boolean containsKeys(Set<? extends K> keys) throws TransactionException {
            return false;
        }

        @Override
        public IgniteFuture<Boolean> containsKeysAsync(Set<? extends K> keys) throws TransactionException {
            return null;
        }

        @Override
        public void put(K key, V val) throws TransactionException {
            internalMap.put(key, val);
        }

        @Override
        public IgniteFuture<Void> putAsync(K key, V val) throws TransactionException {
            return null;
        }

        @Override
        public V getAndPut(K key, V val) throws TransactionException {
            return null;
        }

        @Override
        public IgniteFuture<V> getAndPutAsync(K key, V val) throws TransactionException {
            return null;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> map) throws TransactionException {

        }

        @Override
        public IgniteFuture<Void> putAllAsync(Map<? extends K, ? extends V> map) throws TransactionException {
            return null;
        }

        @Override
        public boolean putIfAbsent(K key, V val) throws TransactionException {
            return false;
        }

        @Override
        public IgniteFuture<Boolean> putIfAbsentAsync(K key, V val) {
            return null;
        }

        @Override
        public boolean remove(K key) throws TransactionException {
            internalMap.remove(key);
            return true;
        }

        @Override
        public IgniteFuture<Boolean> removeAsync(K key) throws TransactionException {
            return null;
        }

        @Override
        public boolean remove(K key, V oldVal) throws TransactionException {
            return false;
        }

        @Override
        public IgniteFuture<Boolean> removeAsync(K key, V oldVal) throws TransactionException {
            return null;
        }

        @Override
        public V getAndRemove(K key) throws TransactionException {
            return null;
        }

        @Override
        public IgniteFuture<V> getAndRemoveAsync(K key) throws TransactionException {
            return null;
        }

        @Override
        public boolean replace(K key, V oldVal, V newVal) throws TransactionException {
            return false;
        }

        @Override
        public IgniteFuture<Boolean> replaceAsync(K key, V oldVal, V newVal) throws TransactionException {
            return null;
        }

        @Override
        public boolean replace(K key, V val) throws TransactionException {
            return false;
        }

        @Override
        public IgniteFuture<Boolean> replaceAsync(K key, V val) throws TransactionException {
            return null;
        }

        @Override
        public V getAndReplace(K key, V val) throws TransactionException {
            return null;
        }

        @Override
        public IgniteFuture<V> getAndReplaceAsync(K key, V val) {
            return null;
        }

        @Override
        public void removeAll(Set<? extends K> keys) throws TransactionException {

        }

        @Override
        public IgniteFuture<Void> removeAllAsync(Set<? extends K> keys) throws TransactionException {
            return null;
        }

        @Override
        public void removeAll() {

        }

        @Override
        public IgniteFuture<Void> removeAllAsync() {
            return null;
        }

        @Override
        public void clear() {

        }

        @Override
        public IgniteFuture<Void> clearAsync() {
            return null;
        }

        @Override
        public void clear(K key) {

        }

        @Override
        public IgniteFuture<Void> clearAsync(K key) {
            return null;
        }

        @Override
        public void clearAll(Set<? extends K> keys) {

        }

        @Override
        public IgniteFuture<Void> clearAllAsync(Set<? extends K> keys) {
            return null;
        }

        @Override
        public void localClear(K key) {

        }

        @Override
        public void localClearAll(Set<? extends K> keys) {

        }

        @Override
        public <T> T invoke(K key, EntryProcessor<K, V, T> entryProcessor, Object... arguments)
            throws TransactionException {
            return null;
        }

        @Override
        public <T> IgniteFuture<T> invokeAsync(K key, EntryProcessor<K, V, T> entryProcessor, Object... arguments)
            throws TransactionException {
            return null;
        }

        @Override
        public <T> T invoke(K key, CacheEntryProcessor<K, V, T> entryProcessor, Object... arguments)
            throws TransactionException {
            return null;
        }

        @Override
        public <T> IgniteFuture<T> invokeAsync(K key, CacheEntryProcessor<K, V, T> entryProcessor, Object... arguments)
            throws TransactionException {
            return null;
        }

        @Override
        public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> keys,
            EntryProcessor<K, V, T> entryProcessor, Object... args) throws TransactionException {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public CacheManager getCacheManager() {
            return null;
        }

        @Override
        public <T> IgniteFuture<Map<K, EntryProcessorResult<T>>> invokeAllAsync(Set<? extends K> keys,
            EntryProcessor<K, V, T> entryProcessor, Object... args) throws TransactionException {
            return null;
        }

        @Override
        public <T> Map<K, EntryProcessorResult<T>> invokeAll(Set<? extends K> keys,
            CacheEntryProcessor<K, V, T> entryProcessor, Object... args) throws TransactionException {
            return null;
        }

        @Override
        public <T> IgniteFuture<Map<K, EntryProcessorResult<T>>> invokeAllAsync(Set<? extends K> keys,
            CacheEntryProcessor<K, V, T> entryProcessor, Object... args) throws TransactionException {
            return null;
        }

        @Override
        public void close() {

        }

        @Override
        public boolean isClosed() {
            return false;
        }

        @Override
        public <T> T unwrap(Class<T> aClass) {
            return null;
        }

        @Override
        public void registerCacheEntryListener(CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {

        }

        @Override
        public void deregisterCacheEntryListener(
            CacheEntryListenerConfiguration<K, V> cacheEntryListenerConfiguration) {

        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
            return null;
        }

        @Override
        public void destroy() {

        }

        @Override
        public IgniteFuture<Boolean> rebalance() {
            return null;
        }

        @Override
        public IgniteFuture<?> indexReadyFuture() {
            return null;
        }

        @Override
        public CacheMetrics metrics() {
            return null;
        }

        @Override
        public CacheMetrics metrics(ClusterGroup grp) {
            return null;
        }

        @Override
        public CacheMetrics localMetrics() {
            return null;
        }

        @Override
        public CacheMetricsMXBean mxBean() {
            return null;
        }

        @Override
        public CacheMetricsMXBean localMxBean() {
            return null;
        }

        @Override
        public Collection<Integer> lostPartitions() {
            return null;
        }

        @Override
        public void enableStatistics(boolean enabled) {

        }

        @Override
        public void clearStatistics() {

        }

        @Override
        public void preloadPartition(int partition) {

        }

        @Override
        public IgniteFuture<Void> preloadPartitionAsync(int partition) {
            return null;
        }

        @Override
        public boolean localPreloadPartition(int partition) {
            return false;
        }
    }
}
