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

package org.opennms.horizon.server.config;

import io.leangen.graphql.spqr.spring.autoconfigure.DataLoaderRegistryFactory;
import lombok.RequiredArgsConstructor;
import org.dataloader.BatchLoader;
import org.dataloader.DataLoader;
import org.dataloader.DataLoaderRegistry;
import org.opennms.horizon.server.mapper.MonitoringLocationMapper;
import org.opennms.horizon.server.model.inventory.MonitoringLocation;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

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
        return CompletableFuture.completedFuture(
            inventoryClient.listLocationsByIds(locationKeys)
                .stream().map(monitoringLocationMapper::protoToLocation).toList());
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
