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
package org.opennms.horizon.timeseries.cortex;

import java.util.Objects;
import java.util.StringJoiner;

public class CortexTSSConfig {
    private final String writeUrl;
    private final int maxConcurrentHttpConnections;
    private final long writeTimeoutInMs;
    private final long readTimeoutInMs;
    private final long bulkheadMaxWaitDurationInMs;
    private final String organizationId;
    private final boolean hasOrganizationId;

    public CortexTSSConfig() {
        this(builder());
    }

    public CortexTSSConfig(Builder builder) {
        this.writeUrl = Objects.requireNonNull(builder.writeUrl);
        this.maxConcurrentHttpConnections = builder.maxConcurrentHttpConnections;
        this.writeTimeoutInMs = builder.writeTimeoutInMs;
        this.readTimeoutInMs = builder.readTimeoutInMs;
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
            final int maxConcurrentHttpConnections,
            final long writeTimeoutInMs,
            final long readTimeoutInMs,
            final long bulkheadMaxWaitDurationInMs,
            final String organizationId) {
        this(builder()
                .writeUrl(writeUrl)
                .maxConcurrentHttpConnections(maxConcurrentHttpConnections)
                .writeTimeoutInMs(writeTimeoutInMs)
                .readTimeoutInMs(readTimeoutInMs)
                .bulkheadMaxWaitDurationInMs(bulkheadMaxWaitDurationInMs)
                .organizationId(organizationId));
    }

    public String getWriteUrl() {
        return writeUrl;
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
        private int maxConcurrentHttpConnections = 100;
        private long writeTimeoutInMs = 1000;
        private long readTimeoutInMs = 1000;
        private long bulkheadMaxWaitDurationInMs = Long.MAX_VALUE;
        private String organizationId = null;

        public Builder writeUrl(final String writeUrl) {
            this.writeUrl = writeUrl;
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
                .add("maxConcurrentHttpConnections=" + maxConcurrentHttpConnections)
                .add("writeTimeoutInMs=" + writeTimeoutInMs)
                .add("readTimeoutInMs=" + readTimeoutInMs)
                .add("bulkheadMaxWaitDurationInMs=" + bulkheadMaxWaitDurationInMs)
                .add("organizationId=" + organizationId)
                .toString();
    }
}
