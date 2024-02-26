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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public abstract class HydraTest {

    protected abstract Hydra<Integer> spawn();

    @Test
    public void testSingleQueue() throws InterruptedException {
        final var hydra = this.spawn();
        final var queue = hydra.queue();

        queue.put(23);
        assertEquals(23, queue.take());

        queue.put(13);
        queue.put(37);
        assertEquals(13, queue.take());
        assertEquals(37, queue.take());
    }

    @Test
    public void testGlobalQueue() throws InterruptedException {
        final var hydra = this.spawn();
        final var queue = hydra.queue();

        queue.put(23);
        assertEquals(23, queue.take());
        assertNull(hydra.poll());

        queue.put(13);
        queue.put(37);
        assertEquals(13, hydra.poll());
        assertEquals(13, queue.take());
        assertEquals(37, queue.take());
        assertNull(hydra.poll());
    }

    @Test
    public void testMultiQueue() throws InterruptedException {
        final var hydra = this.spawn();
        final var queue1 = hydra.queue();
        final var queue2 = hydra.queue();
        final var queue3 = hydra.queue();

        queue1.put(23);
        queue2.put(13);
        queue2.put(37);
        queue3.put(42);

        assertEquals(23, queue1.take());

        assertEquals(13, hydra.poll());
        assertEquals(37, hydra.poll());

        assertEquals(13, queue2.take());
        assertEquals(37, queue2.take());

        assertEquals(42, queue3.take());
        assertNull(hydra.poll());
    }
}
