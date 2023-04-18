package org.opennms.horizon.minion.grpc.queue;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.nio.file.Path;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;
import org.opennms.horizon.shared.ipc.sink.api.AsyncPolicy;
import org.opennms.horizon.shared.ipc.sink.api.SinkModule;

public class OffHeapSendQueueFactoryTest {

    private OffHeapSendQueueFactory factory;

    @BeforeEach
    public void setup(@TempDir Path folder) throws Exception {
        this.factory = new OffHeapSendQueueFactory(folder, 5, 10);
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
        queue.enqueue("5".getBytes());
        queue.enqueue("6".getBytes());
        queue.enqueue("7".getBytes());
        queue.enqueue("8".getBytes());
        queue.enqueue("9".getBytes());

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
