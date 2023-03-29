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
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.opennms.horizon.shared.ipc.sink.api.SendQueue;
import org.opennms.horizon.shared.ipc.sink.api.SendQueueFactory;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.rocksdb.RocksDBException;

import com.google.protobuf.Message;

public class OffHeapSendQueueFactory implements SendQueueFactory {

//    private final int blockSize;
//
//    private final ArrayBlockingQueue<DataBlock.Container> blocks;
//
//    private final ArrayBlockingQueue<DataBlock.Container> memoryBlocks;
//
//    private final RocksDB rocksDB;

    /** Atomic block ID.
     * Used to resolve same-time block ID conflicts.
     */
    private final AtomicLong blockId = new AtomicLong(0);

    public OffHeapSendQueueFactory() throws IOException, RocksDBException {
//        this.blockSize = 4096;
//
//        this.blocks = new ArrayBlockingQueue<>(4096 * 1024);
//        this.memoryBlocks = new ArrayBlockingQueue<>(1024);
//
//        final var options = new Options();
//        options.setCreateIfMissing(true);
//        options.setCompressionType(CompressionType.SNAPPY_COMPRESSION);
//        options.setEnableBlobFiles(true);
//        options.setEnableBlobGarbageCollection(true);
//        options.setMinBlobSize(100_000L);
//        options.setBlobCompressionType(CompressionType.SNAPPY_COMPRESSION);
////        options.setWriteBufferSize(writeBufferSize);
////        options.setBlobFileSize(options.writeBufferSize());
//        options.setTargetFileSizeBase(64L * 1024L * 1024L);
//        options.setMaxBytesForLevelBase(options.targetFileSizeBase() * 10L);
//
//        options.setMaxBackgroundJobs(Math.max(Runtime.getRuntime().availableProcessors(), 3));
//
////        final var cache = new LRUCache(cacheSize, 8);
////        options.setRowCache(cache);
//
//        final var path = Paths.get("./sink/queue").toAbsolutePath();
//        Files.createDirectories(path);
//
//        this.rocksDB = RocksDB.open(options, path.toString());
    }

    @Override
    public <T extends Message> SendQueue<T> createQueue(final SinkModule<?, T> module) {
        // TODO fooker: Do we need to cache here
        return new OffHeapSendQueue<>(module);
    }

    private class OffHeapSendQueue<T extends Message> implements SendQueue<T> {

        private final SinkModule<?, T> module;

        private final Prefix prefix;

        /**
         * Tail of the buffer queue.
         * The stage is written until enough elements for a memory block are available.
         */
        private final ArrayBlockingQueue<byte[]> staging;

        public OffHeapSendQueue(final SinkModule<?, T> module) {
            this.module = Objects.requireNonNull(module);
            this.prefix = new Prefix(module.getId());

            this.staging = new ArrayBlockingQueue<>(10);
        }

        @Override
        public void enqueue(final byte[] message) {
            synchronized (this) {
                try {
                    this.staging.put(message);
                } catch (final InterruptedException e) {
                    // Ignored
                }

//                if (this.staging.size() >= OffHeapSendQueueFactory.this.blockSize) {
//                    // Staging is full - create a new block
//                    final var blockKey = this.prefix.with(
//                        Longs.toByteArray(System.currentTimeMillis()),
//                        Longs.toByteArray(OffHeapSendQueueFactory.this.blockId.getAndIncrement()));
//
//                    final var block = DataBlock.memory(blockKey, this.staging);
//                    this.staging.clear();
//
//                    try {
//                        blocks.put(block);
//                    } catch (InterruptedException e) {
//                        throw new WriteFailedException(e);
//                    }
//
//                    while (!OffHeapSendQueueFactory.this.memoryBlocks.offer(block)) {
//                        // We can't accept a new memory block - move some old ones to off-heap
//                        try {
//                            final var offBlock = OffHeapSendQueueFactory.this.memoryBlocks.take();
//                            offBlock.toOffHeap();
//                        } catch (final InterruptedException e) {
//                            throw new WriteFailedException(e);
//                        }
//                    }
//                }
            }
        }

        @Override
        public byte[] dequeue() throws InterruptedException {
            return this.staging.take();
        }

        @Override
        public boolean isFull() {
            // TODO fooker: Implement this correctly
            return false;
        }

        @Override
        public void close() throws Exception {
            // TODO fooker: persist all blocks
        }
    }
}
