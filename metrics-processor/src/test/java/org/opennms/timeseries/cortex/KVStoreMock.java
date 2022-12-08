package org.opennms.timeseries.cortex;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;

import org.opennms.integration.api.v1.distributed.KeyValueStore;
import org.testcontainers.shaded.org.apache.commons.lang3.NotImplementedException;

public class KVStoreMock implements KeyValueStore {
    private final Map<String, Object> kvStore = new HashMap<>();

    @Override
    public long put(String key, Object value, String context) {
        kvStore.put(key, value);
        return 0L;
    }

    @Override
    public long put(String key, Object value, String context, Integer ttlInSeconds) {
        kvStore.put(key, value);
        return 0;
    }

    @Override
    public Optional get(String key, String context) {
        if (kvStore.get(key) != null)
            return Optional.of(kvStore.get(key));
        else return Optional.empty();
    }

    @Override
    public Optional getIfStale(String key, String context, long timestamp) {
        throw new NotImplementedException();
    }

    @Override
    public OptionalLong getLastUpdated(String key, String context) {
        throw new NotImplementedException();
    }

    @Override
    public Map enumerateContext(String context) {
        return kvStore;
    }

    @Override
    public void delete(String key, String context) {
        throw new NotImplementedException();
    }

    @Override
    public void truncateContext(String context) {
        throw new NotImplementedException();
    }

    @Override
    public CompletableFuture<Long> putAsync(String key, Object value, String context) {
        kvStore.put(key, value);
        return CompletableFuture.completedFuture(0L);
    }

    @Override
    public CompletableFuture<Long> putAsync(String key, Object value, String context, Integer ttlInSeconds) {
        kvStore.put(key, value);
        return CompletableFuture.completedFuture(0L);
    }

    @Override
    public CompletableFuture<Optional> getAsync(String key, String context) {
        throw new NotImplementedException();
    }

    @Override
    public CompletableFuture<Optional> getIfStaleAsync(String key, String context, long timestamp) {
        throw new NotImplementedException();
    }

    @Override
    public CompletableFuture<OptionalLong> getLastUpdatedAsync(String key, String context) {
        throw new NotImplementedException();
    }

    @Override
    public String getName() {
        return this.getClass().getCanonicalName();
    }

    @Override
    public CompletableFuture<Map> enumerateContextAsync(String context) {
        return CompletableFuture.completedFuture(kvStore);
    }

    @Override
    public CompletableFuture<Void> deleteAsync(String key, String context) {
        throw new NotImplementedException();
    }

    @Override
    public CompletableFuture<Void> truncateContextAsync(String context) {
        throw new NotImplementedException();
    }

}
