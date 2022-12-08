package org.opennms.timeseries.cortex;

import java.util.Objects;
import java.util.StringJoiner;

public class CortexTSSConfig {
    private final String writeUrl;
    private final String readUrl;
    private final int maxConcurrentHttpConnections;
    private final long writeTimeoutInMs;
    private final long readTimeoutInMs;
    private final long metricCacheSize;
    private final long externalTagsCacheSize;
    private final long bulkheadMaxWaitDurationInMs;
    private final String organizationId;
    private final boolean hasOrganizationId;

    public CortexTSSConfig() {
        this(builder());
    }

    public CortexTSSConfig(Builder builder) {
        this.writeUrl = Objects.requireNonNull(builder.writeUrl);
        this.readUrl = Objects.requireNonNull(builder.readUrl);
        this.maxConcurrentHttpConnections = builder.maxConcurrentHttpConnections;
        this.writeTimeoutInMs = builder.writeTimeoutInMs;
        this.readTimeoutInMs = builder.readTimeoutInMs;
        this.metricCacheSize = builder.metricCacheSize;
        this.externalTagsCacheSize = builder.externalTagsCacheSize;
        this.bulkheadMaxWaitDurationInMs = builder.bulkheadMaxWaitDurationInMs;
        this.organizationId = builder.organizationId;
        this.hasOrganizationId = organizationId != null && organizationId.trim().length() > 0;
    }

    /**
     * Will be called via blueprint. The builder can be called when not running as Osgi plugin.
     */
    @SuppressWarnings("java:S107")
    public CortexTSSConfig(
        final String writeUrl,
        final String readUrl,
        final int maxConcurrentHttpConnections,
        final long writeTimeoutInMs,
        final long readTimeoutInMs,
        final long metricCacheSize,
        final long externalTagsCacheSize,
        final long bulkheadMaxWaitDurationInMs,
        final String organizationId) {
        this(builder()
            .writeUrl(writeUrl)
            .readUrl(readUrl)
            .maxConcurrentHttpConnections(maxConcurrentHttpConnections)
            .writeTimeoutInMs(writeTimeoutInMs)
            .readTimeoutInMs(readTimeoutInMs)
            .metricCacheSize(metricCacheSize)
            .externalCacheSize(externalTagsCacheSize)
            .bulkheadMaxWaitDurationInMs(bulkheadMaxWaitDurationInMs)
            .organizationId(organizationId));
    }

    public String getWriteUrl() {
        return writeUrl;
    }

    public String getReadUrl() {
        return readUrl;
    }

    public int getMaxConcurrentHttpConnections() {
        return maxConcurrentHttpConnections;
    }

    public long getWriteTimeoutInMs() {
        return writeTimeoutInMs;
    }

    public long getReadTimeoutInMs() {
        return readTimeoutInMs;
    }

    public long getMetricCacheSize() {
        return metricCacheSize;
    }

    public long getExternalTagsCacheSize() {
        return externalTagsCacheSize;
    }

    public long getBulkheadMaxWaitDurationInMs() {
        return bulkheadMaxWaitDurationInMs;
    }

    public boolean hasOrganizationId() {
        return hasOrganizationId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String writeUrl = "http://localhost:9009/api/prom/push";
        private String readUrl = "http://localhost:9009/prometheus/api/v1";
        private int maxConcurrentHttpConnections = 100;
        private long writeTimeoutInMs = 1000;
        private long readTimeoutInMs = 1000;
        private long metricCacheSize = 1000;
        private long externalTagsCacheSize = 1000;
        private long bulkheadMaxWaitDurationInMs = Long.MAX_VALUE;
        private String organizationId = null;

        public Builder writeUrl(final String writeUrl) {
            this.writeUrl = writeUrl;
            return this;
        }

        public Builder readUrl(final String readUrl) {
            this.readUrl = readUrl;
            return this;
        }

        public Builder maxConcurrentHttpConnections(final int maxConcurrentHttpConnections) {
            this.maxConcurrentHttpConnections = maxConcurrentHttpConnections;
            return this;
        }

        public Builder writeTimeoutInMs(final long writeTimeoutInMs) {
            this.writeTimeoutInMs = writeTimeoutInMs;
            return this;
        }

        public Builder readTimeoutInMs(final long readTimeoutInMs) {
            this.readTimeoutInMs = readTimeoutInMs;
            return this;
        }

        public Builder metricCacheSize(final long metricCacheSize) {
            this.metricCacheSize = metricCacheSize;
            return this;
        }

        public Builder externalCacheSize(final long externalTagsCacheSize) {
            this.externalTagsCacheSize = externalTagsCacheSize;
            return this;
        }

        public Builder bulkheadMaxWaitDurationInMs(final long bulkheadMaxWaitDurationInMs) {
            this.bulkheadMaxWaitDurationInMs = bulkheadMaxWaitDurationInMs;
            return this;
        }

        public Builder organizationId(final String organizationId) {
            this.organizationId = organizationId;
            return this;
        }

        public CortexTSSConfig build() {
            return new CortexTSSConfig(this);
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CortexTSSConfig.class.getSimpleName() + "[", "]")
            .add("writeUrl='" + writeUrl + "'")
            .add("readUrl='" + readUrl + "'")
            .add("maxConcurrentHttpConnections=" + maxConcurrentHttpConnections)
            .add("writeTimeoutInMs=" + writeTimeoutInMs)
            .add("readTimeoutInMs=" + readTimeoutInMs)
            .add("metricCacheSize=" + metricCacheSize)
            .add("externalCacheSize=" + externalTagsCacheSize)
            .add("bulkheadMaxWaitDurationInMs=" + bulkheadMaxWaitDurationInMs)
            .add("organizationId=" + organizationId)
            .toString();
    }
}
