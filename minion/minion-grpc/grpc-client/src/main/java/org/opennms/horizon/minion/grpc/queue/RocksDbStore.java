/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.grpc.queue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.CompressionType;
import org.rocksdb.DBOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;


public class RocksDbStore implements SwappingSendQueueFactory.StoreManager {

    private final static DBOptions DB_OPTIONS = new DBOptions()
        .setCreateIfMissing(true)
        .setMaxBackgroundJobs(Math.max(Runtime.getRuntime().availableProcessors(), 3))
        ;

    private final static ColumnFamilyOptions CF_OPTIONS = new ColumnFamilyOptions()
        .setEnableBlobFiles(true)
        .setEnableBlobGarbageCollection(true)
        .setMinBlobSize(16L * 1024L)
        .setBlobFileSize(64L * 1024L * 1024L)
        .setTargetFileSizeBase(64L * 1024L * 1024L)
        .setCompressionType(CompressionType.SNAPPY_COMPRESSION)
        .setBlobCompressionType(CompressionType.SNAPPY_COMPRESSION)
        .setEnableBlobGarbageCollection(true)
        ;

    private final RocksDB db;

    private final Map<Prefix, ColumnFamilyHandle> cfHandles;

    public RocksDbStore() throws RocksDBException, IOException {
        this(Paths.get("./sink/queue").toAbsolutePath());
    }

    public RocksDbStore(final Path path) throws IOException, RocksDBException {
        Files.createDirectories(path);

        // This wired interface works by first querying the available column family names and then pushing the list of
        // descriptors build from these names to the open call, which will fill a list of handles co-indexed with the
        // requested descriptors. That's some wired 90' C list management shit.
        final var cfDescs = RocksDB.listColumnFamilies(new Options(), path.toString()).stream()
                                   .map(columnFamilyName -> new ColumnFamilyDescriptor(columnFamilyName, CF_OPTIONS))
                                   .collect(Collectors.toList());
        cfDescs.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY));

        final var cfHandles = Lists.<ColumnFamilyHandle>newArrayListWithCapacity(cfDescs.size());

        this.db = RocksDB.open(DB_OPTIONS, path.toString(),
                               cfDescs, cfHandles);

        this.cfHandles = Streams.zip(cfDescs.stream(), cfHandles.stream(), Map::entry)
                                .collect(Collectors.toMap(e -> new Prefix(e.getKey().getName()), Map.Entry::getValue));
    }

    public synchronized SwappingSendQueueFactory.Store getStore(final Prefix prefix) throws IOException {
        var cfHandle = this.cfHandles.get(prefix);
        if (cfHandle == null) {
            try {
                cfHandle = this.db.createColumnFamily(new ColumnFamilyDescriptor(prefix.getBytes(), CF_OPTIONS));
            } catch (final RocksDBException e) {
                throw new IOException(e);
            }
            this.cfHandles.put(prefix, cfHandle);
        }

        return new Store(cfHandle);
    }

    @Override
    public void close() {
        this.db.close();
    }

    private class Store implements SwappingSendQueueFactory.Store {

        private final ColumnFamilyHandle cf;

        private Store(final ColumnFamilyHandle cf) {
            this.cf = Objects.requireNonNull(cf);
        }

        @Override
        public byte[] get(final byte[] key) throws IOException {
            try {
                final var result = RocksDbStore.this.db.get(this.cf, key);
                RocksDbStore.this.db.singleDelete(this.cf, key);
                return result;
            } catch (RocksDBException e) {
                throw new IOException(e);
            }
        }

        @Override
        public void put(final byte[] key, final byte[] message) throws IOException {
            try {
                RocksDbStore.this.db.put(this.cf, key, message);
            } catch (RocksDBException e) {
                throw new IOException(e);
            }
        }

        @Override
        public void iterate(final Consumer<byte[]> f) {
            try (final var it = RocksDbStore.this.db.newIterator(this.cf)) {
                it.seekToFirst();

                while (it.isValid()) {
                    f.accept(it.key());
                    it.next();
                }
            }
        }

        @Override
        public void close() {
        }
    }
}
