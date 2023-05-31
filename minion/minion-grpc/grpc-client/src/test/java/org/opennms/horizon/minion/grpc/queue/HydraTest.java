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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public abstract class HydraTest {

    protected abstract Hydra<Integer> spawn();

    @Test
    public void testSingleQueue() throws InterruptedException {
        final var hydra = this.spawn();
        final var queue = hydra.queue();

        assertTrue(queue.isEmpty());

        queue.put(23);
        assertEquals(23, queue.take());

        queue.put(13);
        queue.put(37);
        assertFalse(queue.isEmpty());

        assertEquals(13, queue.take());
        assertEquals(37, queue.take());

        assertTrue(queue.isEmpty());
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

        assertTrue(queue1.isEmpty());
        assertTrue(queue2.isEmpty());
        assertTrue(queue3.isEmpty());

        queue1.put(23);

        assertFalse(queue1.isEmpty());
        assertTrue(queue2.isEmpty());
        assertTrue(queue3.isEmpty());

        queue2.put(13);
        queue2.put(37);

        assertFalse(queue1.isEmpty());
        assertFalse(queue2.isEmpty());
        assertTrue(queue3.isEmpty());

        queue3.put(42);

        assertFalse(queue1.isEmpty());
        assertFalse(queue2.isEmpty());
        assertFalse(queue3.isEmpty());

        assertEquals(23, queue1.take());

        assertTrue(queue1.isEmpty());
        assertFalse(queue2.isEmpty());
        assertFalse(queue3.isEmpty());

        assertEquals(13, hydra.poll());
        assertEquals(37, hydra.poll());

        assertEquals(13, queue2.take());
        assertEquals(37, queue2.take());

        assertTrue(queue1.isEmpty());
        assertTrue(queue2.isEmpty());
        assertFalse(queue3.isEmpty());

        assertEquals(42, queue3.take());
        assertNull(hydra.poll());

        assertTrue(queue1.isEmpty());
        assertTrue(queue2.isEmpty());
        assertTrue(queue3.isEmpty());
    }

}
