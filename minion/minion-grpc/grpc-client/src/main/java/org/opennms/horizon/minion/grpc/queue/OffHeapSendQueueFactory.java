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
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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

    private final Semaphore memorySemaphore;
    private final Semaphore totalSemaphore;

    private final Hydra<Element> hydra = new Hydra<>();

    public OffHeapSendQueueFactory(final int memoryElements, final int offHeapElements) throws IOException, RocksDBException {
        this(Paths.get("./sink/queue").toAbsolutePath(), memoryElements, offHeapElements);
    }

    public OffHeapSendQueueFactory(
        final Path path,
        final int memoryElements,
        final int offHeapElements
    ) throws IOException, RocksDBException {
        this.memorySemaphore = new Semaphore(memoryElements);
        this.totalSemaphore = new Semaphore(memoryElements + offHeapElements);

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

        private final Hydra<Element>.SubQueue elements;

        public OffHeapSendQueue(final SinkModule<?, T> module) {
            this.prefix = new Prefix(module.getId());

            this.elements = OffHeapSendQueueFactory.this.hydra.queue();

            try (final var it = OffHeapSendQueueFactory.this.rocksDB.newIterator()) {
                it.seek(prefix.getBytes());

                while (it.isValid() && prefix.of(it.key())) {
                    try {
                        this.elements.put(new Element(Bytes.concat(it.key())));
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
            // Block until new element can be accepted
            OffHeapSendQueueFactory.this.totalSemaphore.acquire();

            // Persist elements until memory block becomes available
            while (!OffHeapSendQueueFactory.this.memorySemaphore.tryAcquire()) {
                // Move the oldest memory block to off-heap
                try {
                    final var memoryBlock = OffHeapSendQueueFactory.this.hydra.poll();
                    if (memoryBlock == null) {
                        Thread.yield();
                        continue;
                    }

                    memoryBlock.persist();

                    OffHeapSendQueueFactory.this.memorySemaphore.release();
                } catch (final IOException e) {
                    // TODO fooker: Add exception to method signature
                    throw new RuntimeException(e);
                }
            }

            // Now we have room in memory for a block
            final var key = this.prefix.with(
                Longs.toByteArray(System.currentTimeMillis()),
                Longs.toByteArray(OffHeapSendQueueFactory.this.blockId.getAndIncrement()));

            final var newElement = new Element(key, message);

            this.elements.put(newElement);
        }

        @Override
        public byte[] dequeue() throws InterruptedException {
            try {
                final var element = this.elements.take();

                OffHeapSendQueueFactory.this.totalSemaphore.release();
                if (element.isInMemory()) {
                    OffHeapSendQueueFactory.this.memorySemaphore.release();
                }

                return element.read();
            } catch (final IOException ex) {
                // TODO fooker: Add exception to method signature
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void close() throws Exception {
            for (Element element = this.elements.poll(); element != null; element = this.elements.poll()) {
                element.persist();
            }
        }

        @Override
        public String toString() {
            return this.prefix.toString();
        }
    }

    public class Element {
        public final byte[] key;

        private final byte[] message;

        /** Lock for database access. **/
        private final Lock lock = new ReentrantLock();

        public Element(final byte[] key,
                       final byte[] message) {
            this.key = Objects.requireNonNull(key);
            this.message = Objects.requireNonNull(message);
        }

        public Element(final byte[] key) {
            this.key = Objects.requireNonNull(key);
            this.message = null;
        }

        private void persist() throws IOException {
            this.lock.lock();
            try {
                if (this.message == null) {
                    // Already persisted
                    return;
                }

                try {
                    OffHeapSendQueueFactory.this.rocksDB.put(key, this.message);
                } catch (RocksDBException e) {
                    throw new IOException(e);
                }
            } finally {
                this.lock.unlock();
            }
        }

        private byte[] read() throws IOException {
            this.lock.lock();
            try {
                if (this.message != null) {
                    // Still in memory
                    return this.message;
                }

                try {
                    return OffHeapSendQueueFactory.this.rocksDB.get(this.key);
                } catch (final RocksDBException e) {
                    throw new IOException(e);
                }
            } finally {
                this.lock.unlock();
            }
        }

        public boolean isInMemory() {
            this.lock.lock();
            try {
                return this.message != null;
            } finally {
                this.lock.unlock();
            }
        }
    }

    public int getMemoryPermits() {
        return this.memorySemaphore.availablePermits();
    }

    public int getTotalPermits() {
        return this.totalSemaphore.availablePermits();
    }
}
