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

/**
 * A multi-headed queue.
 *
 * A hydra consists of a global queue and multiple sub-queues. Pushing elements to a hydra is always done using a
 * sub-queue. The added elements will then be added to the sub-queue and the global queue. Taking elements from a
 * sub-queue will also remove that element from the global queue, even if they are not at the head of the global queue.
 * In addition, the global queue can be polled for elements. Elements directly taken from the global queue will still
 * remain in the sub-queue which have been used to enqueue that element.
 */
public interface Hydra<E> {
    E poll();

    SubQueue<E> queue();

    interface SubQueue<E> {
        public E take() throws InterruptedException;

        public E poll();

        public void put(final E element) throws InterruptedException;

        public boolean isEmpty();
    }
}
