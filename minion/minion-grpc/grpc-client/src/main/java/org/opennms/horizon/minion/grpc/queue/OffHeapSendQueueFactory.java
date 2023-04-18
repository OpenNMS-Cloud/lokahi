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

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.opennms.horizon.shared.ipc.sink.api.SendQueue;
import org.opennms.horizon.shared.ipc.sink.api.SendQueueFactory;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;
import org.rocksdb.CompressionType;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;

public class OffHeapSendQueueFactory implements SendQueueFactory, Closeable {

    private final Options rocksDBOptions;
    private final RocksDB rocksDB;

    /**
     * Atomic block ID.
     * Used to resolve same-time block ID conflicts.
     */
    private final AtomicLong blockId = new AtomicLong(0);

    private final Semaphore memoryElements;
    private final Semaphore totalElements;

    public OffHeapSendQueueFactory(final int memoryElements, final int offHeapElements) throws IOException, RocksDBException {
        this(Paths.get("./sink/queue").toAbsolutePath(), memoryElements, offHeapElements);
    }

    public OffHeapSendQueueFactory(
        final Path path,
        final int memoryElements,
        final int offHeapElements
    ) throws IOException, RocksDBException {
        this.memoryElements = new Semaphore(memoryElements);
        this.totalElements = new Semaphore(memoryElements + offHeapElements);

        this.rocksDBOptions = new Options()
            .setCreateIfMissing(true)
            .setCompressionType(CompressionType.SNAPPY_COMPRESSION)
            .setEnableBlobFiles(true)
            .setEnableBlobGarbageCollection(true)
            .setMinBlobSize(100_000L)
            .setBlobCompressionType(CompressionType.SNAPPY_COMPRESSION)
            .setCreateMissingColumnFamilies(true)
            .setTargetFileSizeBase(64L * 1024L * 1024L)
            .setMaxBytesForLevelBase(64L * 1024L * 1024L * 10L)
            .setMaxBackgroundJobs(Math.max(Runtime.getRuntime().availableProcessors(), 3));

        Files.createDirectories(path);

        this.rocksDB = RocksDB.open(this.rocksDBOptions, path.toString());
    }

    @Override
    public <T> SendQueue createQueue(final SinkModule<?, T> module) {
        return new OffHeapSendQueue<>(module);
    }

    @Override
    public void close() {
        this.rocksDB.close();
        this.rocksDBOptions.close();
    }

    private class OffHeapSendQueue<T> implements SendQueue {

        private final Prefix prefix;

        private final LinkedBlockingQueue<MemoryElement> memoryElements;
        private final LinkedBlockingQueue<OffHeapElement> offHeapElements;

        private final ReentrantLock lock = new ReentrantLock();

        private final Condition available = this.lock.newCondition();

        public OffHeapSendQueue(final SinkModule<?, T> module) {
            this.prefix = new Prefix(module.getId());

            this.memoryElements = new LinkedBlockingQueue<>();
            this.offHeapElements = new LinkedBlockingQueue<>();

            try (final var it = OffHeapSendQueueFactory.this.rocksDB.newIterator()) {
                it.seek(prefix.getBytes());

                while (it.isValid() && prefix.of(it.key())) {
                    try {
                        this.offHeapElements.put(new OffHeapElement(Bytes.concat(it.key())));
                    } catch (final InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ex);
                    }
                    it.next();
                }
            }
        }

        @Override
        public void enqueue(final byte[] message) throws InterruptedException {
            final var key = this.prefix.with(
                Longs.toByteArray(System.currentTimeMillis()),
                Longs.toByteArray(OffHeapSendQueueFactory.this.blockId.getAndIncrement()));

            final var newBlock = new MemoryElement(key, message);

            this.lock.lock();
            try {
                OffHeapSendQueueFactory.this.totalElements.acquire();

                while (!OffHeapSendQueueFactory.this.memoryElements.tryAcquire()) {
                    // We can't accept a new memory block - move the oldest memory block to off-heap
                    try {
                        final var oldBlock = this.memoryElements.take();
                        OffHeapSendQueueFactory.this.memoryElements.release();

                        final var offHeapBlock = this.persist(oldBlock);
                        this.offHeapElements.put(offHeapBlock);

                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                // Now we have room in memory for a block
                this.memoryElements.put(newBlock);

                this.available.signal();

            } finally {
                this.lock.unlock();
            }
        }

        @Override
        public byte[] dequeue() throws InterruptedException {
            this.lock.lock();
            try {
                while (true) {
                    try {
                        final var key = this.offHeapElements.poll();
                        if (key != null) {
                            OffHeapSendQueueFactory.this.totalElements.release();
                            return this.loadMessage(key);
                        }
                    } catch (final IOException ex) {
                        throw new RuntimeException(ex);
                    }

                    {
                        final var block = this.memoryElements.poll();
                        if (block != null) {
                            OffHeapSendQueueFactory.this.memoryElements.release();
                            OffHeapSendQueueFactory.this.totalElements.release();
                            return block.message;
                        }
                    }

                    this.available.await();
                }

            } finally {
                this.lock.unlock();
            }
        }

        @Override
        public void close() throws Exception {
            for (MemoryElement element = this.memoryElements.poll(); element != null; element = this.memoryElements.poll()) {
                this.persist(element);
            }
        }

        private byte[] loadMessage(final OffHeapElement element) throws IOException {
            try {
                return OffHeapSendQueueFactory.this.rocksDB.get(element.key);
            } catch (final RocksDBException e) {
                throw new IOException(e);
            }
        }

        public OffHeapElement persist(final MemoryElement element) throws IOException {
            final byte[] key = element.key;

            try {
                OffHeapSendQueueFactory.this.rocksDB.put(key, element.message);
            } catch (RocksDBException e) {
                throw new IOException(e);
            }

            return new OffHeapElement(key);
        }

        private static class MemoryElement {
            public final byte[] key;
            public final byte[] message;

            public MemoryElement(final byte[] key,
                                 final byte[] message) {
                this.key = Objects.requireNonNull(key);
                this.message = Objects.requireNonNull(message);
            }
        }

        private static class OffHeapElement {
            public final byte[] key;

            public OffHeapElement(final byte[] key) {
                this.key = Objects.requireNonNull(key);
            }
        }
    }
}
