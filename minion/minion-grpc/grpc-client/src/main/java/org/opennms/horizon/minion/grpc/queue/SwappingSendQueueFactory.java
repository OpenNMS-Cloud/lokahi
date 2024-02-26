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

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Longs;
import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.opennms.horizon.shared.ipc.sink.api.SendQueue;
import org.opennms.horizon.shared.ipc.sink.api.SendQueueFactory;

public class SwappingSendQueueFactory implements SendQueueFactory, Closeable {

    private final StoreManager stores;

    /**
     * Atomic block ID.
     * Used to resolve same-time block ID conflicts.
     */
    private final AtomicLong blockId = new AtomicLong(0);

    private final Semaphore memorySemaphore;
    private final Semaphore totalSemaphore;

    private final Hydra<Element> hydra;

    public SwappingSendQueueFactory(
            final StoreManager stores,
            final int memoryElements,
            final int offHeapElements,
            final Hydra<Element> hydra) {
        this.stores = Objects.requireNonNull(stores);

        this.memorySemaphore = new Semaphore(memoryElements);
        this.totalSemaphore = new Semaphore(memoryElements + offHeapElements);

        this.hydra = Objects.requireNonNull(hydra);
    }

    public SwappingSendQueueFactory(final StoreManager stores, final int memoryElements, final int offHeapElements) {
        this(stores, memoryElements, offHeapElements, new LinkedHydra<>());
    }

    @Override
    public SendQueue createQueue(final String id) throws IOException {
        return new OffHeapSendQueue<>(id);
    }

    @Override
    public void close() throws IOException {
        this.stores.close();
    }

    private class OffHeapSendQueue<T> implements SendQueue {

        private final Store store;

        private final Hydra.SubQueue<Element> elements;

        public OffHeapSendQueue(final String id) throws IOException {
            this.store = SwappingSendQueueFactory.this.stores.getStore(new Prefix(id));

            this.elements = SwappingSendQueueFactory.this.hydra.queue();

            this.store.iterate(key -> {
                try {
                    this.elements.put(new Element(Bytes.concat(key)));
                } catch (final InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ex);
                }
            });
        }

        @Override
        public void enqueue(final byte[] message) throws InterruptedException {
            final var key = Bytes.concat(
                    Longs.toByteArray(System.currentTimeMillis()),
                    Longs.toByteArray(SwappingSendQueueFactory.this.blockId.getAndIncrement()));

            final var newElement = new Element(key, message);

            // Block until new element can be accepted
            SwappingSendQueueFactory.this.totalSemaphore.acquire();

            // Persist elements until memory block becomes available
            while (!SwappingSendQueueFactory.this.memorySemaphore.tryAcquire()) {
                // Move the oldest memory block to off-heap
                try {
                    final var memoryBlock = SwappingSendQueueFactory.this.hydra.poll();
                    if (memoryBlock == null) {
                        Thread.yield();
                        continue;
                    }

                    memoryBlock.persist(this.store);

                    SwappingSendQueueFactory.this.memorySemaphore.release();
                } catch (final IOException e) {
                    // TODO fooker: Add exception to method signature
                    throw new RuntimeException(e);
                }
            }

            // Now we have room in memory for a block
            this.elements.put(newElement);
        }

        @Override
        public byte[] dequeue() throws InterruptedException {
            try {
                final var element = this.elements.take();

                SwappingSendQueueFactory.this.totalSemaphore.release();
                if (element.isInMemory()) {
                    SwappingSendQueueFactory.this.memorySemaphore.release();
                }

                return element.read(this.store);
            } catch (final IOException ex) {
                // TODO fooker: Add exception to method signature
                throw new RuntimeException(ex);
            }
        }

        @Override
        public void close() throws Exception {
            for (Element element = this.elements.poll(); element != null; element = this.elements.poll()) {
                element.persist(this.store);
            }

            this.store.close();
        }
    }

    public class Element {
        public final byte[] key;

        private final byte[] message;

        /** Lock for database access. **/
        private final Lock lock = new ReentrantLock();

        public Element(final byte[] key, final byte[] message) {
            this.key = Objects.requireNonNull(key);
            this.message = Objects.requireNonNull(message);
        }

        public Element(final byte[] key) {
            this.key = Objects.requireNonNull(key);
            this.message = null;
        }

        private void persist(final Store store) throws IOException {
            this.lock.lock();
            try {
                if (this.message == null) {
                    // Already persisted
                    return;
                }

                store.put(key, this.message);
            } finally {
                this.lock.unlock();
            }
        }

        private byte[] read(final Store store) throws IOException {
            this.lock.lock();
            try {
                if (this.message != null) {
                    // Still in memory
                    return this.message;
                }

                return store.get(this.key);
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

    public interface StoreManager extends Closeable {
        Store getStore(final Prefix prefix) throws IOException;
    }

    public interface Store extends Closeable {

        byte[] get(final byte[] key) throws IOException;

        void put(final byte[] key, final byte[] message) throws IOException;

        void iterate(final Consumer<byte[]> f);
    }
}
