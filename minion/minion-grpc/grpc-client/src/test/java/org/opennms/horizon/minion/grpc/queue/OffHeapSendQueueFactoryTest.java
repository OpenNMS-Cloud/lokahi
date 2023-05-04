package org.opennms.horizon.minion.grpc.queue;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.awaitility.Awaitility.await;

import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.SendQueue;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;

public class OffHeapSendQueueFactoryTest {

    public static final int MEMORY_ELEMENTS = 5;
    public static final int OFF_HEAP_ELEMENTS = 10;
    private OffHeapSendQueueFactory factory;

    @BeforeEach
    public void setup(@TempDir Path folder) throws Exception {
        this.factory = new OffHeapSendQueueFactory(folder, MEMORY_ELEMENTS, OFF_HEAP_ELEMENTS);
    }

    @Test
    public void testQueue() throws Exception {
        final var sinkModule = new DummySinkModule("test");
        final var queue = this.factory.createQueue(sinkModule);
        queue.enqueue("lala".getBytes());

        assertArrayEquals("lala".getBytes(), queue.dequeue());
    }

    @Test
    public void testQueueOffHeap() throws Exception {
        final var sinkModule = new DummySinkModule("test");
        final var queue = this.factory.createQueue(sinkModule);
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
        final var sinkModule = new DummySinkModule("test");
        final var queue = this.factory.createQueue(sinkModule);
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

        final var otherQueue = this.factory.createQueue(sinkModule);

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
        final var queueA = this.factory.createQueue(new DummySinkModule("testA"));
        final var queueB = this.factory.createQueue(new DummySinkModule("testB"));

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
            .put("A", this.factory.createQueue(new DummySinkModule("testA")))
            .put("B", this.factory.createQueue(new DummySinkModule("testB")))
            .put("C", this.factory.createQueue(new DummySinkModule("testC")))
            .build();

        final var executor = Executors.newWorkStealingPool(100);

        final var consumedSum = new AtomicLong(0);
        final var producedSum = new AtomicLong(0);

        for (final var queue: queues.entrySet()) {
            IntStream.range(0, 50).forEach(i -> {
                executor.submit((Callable<?>) () -> {
                    System.out.printf("[%s:%d] Starting consumer\n", queue.getKey(), i);

                    for (int cnt = 0; cnt < rounds; cnt++) {
                        final var l = Longs.fromByteArray(queue.getValue().dequeue());
                        consumedSum.addAndGet(l);

                        if (cnt % 1000 == 0) {
                            System.out.printf("[%s:%d] Consumed: %d\n", queue.getKey(), i, cnt);
                        }
                    }

                    return null;
                });
            });
        }

        final List<Future<?>> producers = Lists.newArrayList();

        for (final var queue: queues.entrySet()) {
            IntStream.range(0, 50).forEach(i -> {
                producers.add(executor.submit((Callable<?>) () -> {
                    System.out.printf("[%s:%d] Starting producer\n", queue.getKey(), i);

                    for (int cnt = 0; cnt < rounds; cnt++) {
                        final var l = random.nextLong();

                        queue.getValue().enqueue(Longs.toByteArray(l));
                        producedSum.addAndGet(l);

                        if (cnt % 1000 == 0) {
                            System.out.printf("[%s:%d] Produced: %d\n", queue.getKey(), i, cnt);
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

    private static class DummySinkModule implements SinkModule<String, String> {

        private final String id;

        private DummySinkModule(final String id) {
            this.id = Objects.requireNonNull(id);
        }

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public int getNumConsumerThreads() {
            return 3;
        }

        @Override
        public byte[] marshal(String message) {
            return message.getBytes();
        }

        @Override
        public String unmarshal(byte[] message) {
            return new String(message);
        }

        @Override
        public byte[] marshalSingleMessage(String message) {
            return message.getBytes();
        }

        @Override
        public String unmarshalSingleMessage(byte[] message) {
            return new String(message);
        }

        @Override
        public AggregationPolicy<String, String, ?> getAggregationPolicy() {
            return new AggregationPolicy<String, String, String>() {
                @Override
                public int getCompletionSize() {
                    return 3;
                }

                @Override
                public int getCompletionIntervalMs() {
                    return 1000;
                }

                @Override
                public String key(String message) {
                    return Character.toString(message.charAt(0));
                }

                @Override
                public String aggregate(String accumulator, String newMessage) {
                    if (accumulator == null) {
                        return newMessage;
                    }

                    return accumulator + "|" + newMessage;
                }

                @Override
                public String build(String accumulator) {
                    return accumulator;
                }


            };
        }

        @Override
        public AsyncPolicy getAsyncPolicy() {
            return new AsyncPolicy() {
                @Override
                public int getQueueSize() {
                    return 5;
                }

                @Override
                public int getNumThreads() {
                    return 2;
                }
            };
        }
    }
}
