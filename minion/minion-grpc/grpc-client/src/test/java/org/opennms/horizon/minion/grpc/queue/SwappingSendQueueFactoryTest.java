package org.opennms.horizon.minion.grpc.queue;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opennms.horizon.shared.ipc.sink.api.SendQueue;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;

public class SwappingSendQueueFactoryTest {

    public static final int MEMORY_ELEMENTS = 5;
    public static final int OFF_HEAP_ELEMENTS = 10;
    private SwappingSendQueueFactory factory;

    @BeforeEach
    public void setup(@TempDir Path folder) throws Exception {
        final var store = new RocksDbStore(folder);
        this.factory = new SwappingSendQueueFactory(store, MEMORY_ELEMENTS, OFF_HEAP_ELEMENTS);
    }

    @Test
    public void testQueue() throws Exception {
        final var queue = this.factory.createQueue("test");
        queue.enqueue("lala".getBytes());

        assertArrayEquals("lala".getBytes(), queue.dequeue());
    }

    @Test
    public void testQueueOffHeap() throws Exception {
        final var queue = this.factory.createQueue("test");
        queue.enqueue("0".getBytes());
        queue.enqueue("1".getBytes());
        queue.enqueue("2".getBytes());
        queue.enqueue("3".getBytes());
        queue.enqueue("4".getBytes());

        // No memory permits left - start to persist off-heap
        assertEquals(0, this.factory.getMemoryPermits());
        assertEquals(OFF_HEAP_ELEMENTS, this.factory.getTotalPermits());

        queue.enqueue("5".getBytes());
        queue.enqueue("6".getBytes());
        queue.enqueue("7".getBytes());
        queue.enqueue("8".getBytes());
        queue.enqueue("9".getBytes());

        assertEquals(0, this.factory.getMemoryPermits());
        assertEquals(OFF_HEAP_ELEMENTS - 5, this.factory.getTotalPermits());

        assertArrayEquals("0".getBytes(), queue.dequeue());
        assertArrayEquals("1".getBytes(), queue.dequeue());
        assertArrayEquals("2".getBytes(), queue.dequeue());
        assertArrayEquals("3".getBytes(), queue.dequeue());
        assertArrayEquals("4".getBytes(), queue.dequeue());
        assertArrayEquals("5".getBytes(), queue.dequeue());
        assertArrayEquals("6".getBytes(), queue.dequeue());
        assertArrayEquals("7".getBytes(), queue.dequeue());
        assertArrayEquals("8".getBytes(), queue.dequeue());
        assertArrayEquals("9".getBytes(), queue.dequeue());
    }

    @Test
    public void testQueueRestore() throws Exception {
        final var queue = this.factory.createQueue("test");
        queue.enqueue("0".getBytes());
        queue.enqueue("1".getBytes());
        queue.enqueue("2".getBytes());
        queue.enqueue("3".getBytes());
        queue.enqueue("4".getBytes());
        queue.enqueue("5".getBytes());
        queue.enqueue("6".getBytes());
        queue.enqueue("7".getBytes());
        queue.enqueue("8".getBytes());
        queue.enqueue("9".getBytes());

        queue.close();

        final var otherQueue = this.factory.createQueue("test");

        assertArrayEquals("0".getBytes(), otherQueue.dequeue());
        assertArrayEquals("1".getBytes(), otherQueue.dequeue());
        assertArrayEquals("2".getBytes(), otherQueue.dequeue());
        assertArrayEquals("3".getBytes(), otherQueue.dequeue());
        assertArrayEquals("4".getBytes(), otherQueue.dequeue());
        assertArrayEquals("5".getBytes(), otherQueue.dequeue());
        assertArrayEquals("6".getBytes(), otherQueue.dequeue());
        assertArrayEquals("7".getBytes(), otherQueue.dequeue());
        assertArrayEquals("8".getBytes(), otherQueue.dequeue());
        assertArrayEquals("9".getBytes(), otherQueue.dequeue());
    }

    @Test
    public void testMemoryStealing() throws Exception {
        final var queueA = this.factory.createQueue("testA");
        final var queueB = this.factory.createQueue("testB");

        // Saturate memory limit on first queue
        for (int i = 0; i < MEMORY_ELEMENTS; i++) {
            queueA.enqueue(String.format("test-a-%d", i).getBytes());
        }

        assertEquals(0, this.factory.getMemoryPermits());

        queueB.enqueue("test-b-0".getBytes());

        assertEquals(0, this.factory.getMemoryPermits());
    }

    @Test
    public void testConcurrentReading() throws Exception {
        final var rounds = 100_000;

        final var random = new Random();

        final var queues = ImmutableMap.<String, SendQueue>builder()
            .put("A", this.factory.createQueue("testA"))
            .put("B", this.factory.createQueue("testB"))
            .put("C", this.factory.createQueue("testC"))
            .build();

        final var executor = Executors.newWorkStealingPool(100);

        final var consumedSum = new AtomicLong(0);
        final var producedSum = new AtomicLong(0);

        for (final var queue: queues.entrySet()) {
            IntStream.range(0, 50).forEach(i -> {
                executor.submit((Callable<?>) () -> {
                    for (int cnt = 0; cnt < rounds; cnt++) {
                        final var l = Longs.fromByteArray(queue.getValue().dequeue());
                        consumedSum.addAndGet(l);
                    }

                    return null;
                });
            });
        }

        final List<Future<?>> producers = Lists.newArrayList();

        for (final var queue: queues.entrySet()) {
            IntStream.range(0, 50).forEach(i -> {
                producers.add(executor.submit((Callable<?>) () -> {
                    // System.out.printf("[%s:%d] Starting producer\n", queue.getKey(), i);

                    for (int cnt = 0; cnt < rounds; cnt++) {
                        final var l = random.nextLong();

                        queue.getValue().enqueue(Longs.toByteArray(l));
                        producedSum.addAndGet(l);

                        if (cnt % 1000 == 0) {
                            // System.out.printf("[%s:%d] Produced: %d\n", queue.getKey(), i, cnt);
                        }
                    }

                    return null;
                }));
            });
        }

        for (final var producer : producers) {
            producer.get();
        }

        await().until(() -> consumedSum.get() == producedSum.get());
    }

    @Test
    public void testStoreEviction() throws Exception {
        try (final var queue = this.factory.createQueue("test")) {
            queue.enqueue("0".getBytes());
            queue.enqueue("1".getBytes());
            queue.enqueue("2".getBytes());
            queue.enqueue("3".getBytes());
        }

        try (final var queue = this.factory.createQueue("test")) {
            assertArrayEquals("0".getBytes(), queue.dequeue());
            assertArrayEquals("1".getBytes(), queue.dequeue());
        }

        try (final var queue = this.factory.createQueue("test")) {
            assertArrayEquals("2".getBytes(), queue.dequeue());
            assertArrayEquals("3".getBytes(), queue.dequeue());
            assertTrue(queue.isEmpty());
        }

        try (final var queue = this.factory.createQueue("test")) {
            assertTrue(queue.isEmpty());
        }
    }
}
