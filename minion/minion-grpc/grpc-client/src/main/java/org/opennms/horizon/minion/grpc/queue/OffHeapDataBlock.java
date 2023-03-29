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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.rocksdb.RocksDB;

import com.google.common.primitives.Bytes;

public class OffHeapDataBlock implements DataBlock {

    private final RocksDB db;

    private final byte[] key;

    public OffHeapDataBlock(RocksDB db, byte[] key) {
        this.db = Objects.requireNonNull(db);
        this.key = Objects.requireNonNull(key);
    }

    @Override
    public MemoryDataBlock asMemory() {
        return null;
    }

    @Override
    public OffHeapDataBlock asOffHeap() {
        return this;
    }

//    public static <T> OffHeapDataBlock<T> persist(final RocksDB rocksDB,
//                                                  final byte[] key,
//                                                  final Collection<T> data) {
//
//    }

//    public static  <T> Iterator<OffHeapDataBlock<T>> restore(final RocksDB rocksDB,
//                                                             final String moduleName) {
//        final var prefix = new Prefix(moduleName);
//
//        // Try to restore any existing data from disk
//        try (final var it = rocksDB.newIterator()) {
//            it.seek(prefix.getBytes());
//
//            while (it.isValid() && prefix.isPrefixOf(it.key())) {
//                final var key =
//            }
//
//
//        }
//    }

}
