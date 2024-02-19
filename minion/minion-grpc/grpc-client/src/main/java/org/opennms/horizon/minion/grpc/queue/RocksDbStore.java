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
package org.opennms.horizon.minion.grpc.queue;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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

public class RocksDbStore implements SwappingSendQueueFactory.StoreManager {

    private static final DBOptions DB_OPTIONS = new DBOptions()
            .setCreateIfMissing(true)
            .setMaxBackgroundJobs(Math.max(Runtime.getRuntime().availableProcessors(), 3));

    private static final ColumnFamilyOptions CF_OPTIONS = new ColumnFamilyOptions()
            .setEnableBlobFiles(true)
            .setEnableBlobGarbageCollection(true)
            .setMinBlobSize(16L * 1024L)
            .setBlobFileSize(64L * 1024L * 1024L)
            .setTargetFileSizeBase(64L * 1024L * 1024L)
            .setCompressionType(CompressionType.SNAPPY_COMPRESSION)
            .setBlobCompressionType(CompressionType.SNAPPY_COMPRESSION)
            .setEnableBlobGarbageCollection(true);

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

        if (cfDescs.stream().noneMatch(desc -> Arrays.equals(desc.getName(), RocksDB.DEFAULT_COLUMN_FAMILY))) {
            cfDescs.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY));
        }

        final var cfHandles = Lists.<ColumnFamilyHandle>newArrayListWithCapacity(cfDescs.size());

        this.db = RocksDB.open(DB_OPTIONS, path.toString(), cfDescs, cfHandles);

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
        public void close() {}
    }
}
