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
package org.opennms.horizon.server.config;

import io.leangen.graphql.spqr.spring.autoconfigure.DataLoaderRegistryFactory;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.opennms.horizon.server.mapper.MonitoringLocationMapper;
import org.opennms.horizon.server.model.inventory.MonitoringLocation;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class DataLoaderFactory implements DataLoaderRegistryFactory {
    public static final String DATA_LOADER_LOCATION = "location";
    private final InventoryClient inventoryClient;
    private final MonitoringLocationMapper monitoringLocationMapper;
    private final BatchLoader<Key, MonitoringLocation> locationBatchLoader = this::locations;

    @Override
    public DataLoaderRegistry createDataLoaderRegistry() {
        DataLoader<Key, MonitoringLocation> locationDataLoader = new DataLoader<>(locationBatchLoader);
        DataLoaderRegistry loaders = new DataLoaderRegistry();
        loaders.register(DATA_LOADER_LOCATION, locationDataLoader);
        return loaders;
    }

    private CompletableFuture<List<MonitoringLocation>> locations(List<Key> locationKeys) {
        return CompletableFuture.completedFuture(inventoryClient.listLocationsByIds(locationKeys).stream()
                .map(monitoringLocationMapper::protoToLocation)
                .toList());
    }

    public static class Key {
        private long id;
        private String token;

        public Key(long id, String token) {
            this.id = id;
            this.token = token;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getToken() {
            return token;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Key key = (Key) o;
            return id == key.id && token.equals(key.token);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, token);
        }
    }
}
