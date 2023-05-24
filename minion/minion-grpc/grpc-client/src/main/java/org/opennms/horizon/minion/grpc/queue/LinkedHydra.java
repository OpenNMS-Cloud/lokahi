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

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LinkedHydra<E> implements Hydra<E> {

    private final Lock lock = new ReentrantLock();

    private static class Node<E> {
        public Node<E> globalPrev;
        public Node<E> globalNext;

        public Node<E> localNext;

        public E element;
    }

    private transient Node<E> head;
    private transient Node<E> tail;

    @Override
    public E poll() {
        final var lock = LinkedHydra.this.lock;
        lock.lock();
        try {
            final var curr = this.head;
            if (curr == null) {
                return null;
            }

            this.head = this.head.globalNext;
            if (this.head == null) {
                this.tail = null;
            }

            curr.globalPrev = null;

            return curr.element;

        } finally {
            lock.unlock();
        }
    }

    @Override
    public Hydra.SubQueue<E> queue() {
        return new SubQueue();
    }

    public class SubQueue implements Hydra.SubQueue<E> {

        private final Condition available = LinkedHydra.this.lock.newCondition();

        private transient Node<E> head;
        private transient Node<E> tail;

        @Override
        public E take() throws InterruptedException {
            final var lock = LinkedHydra.this.lock;
            lock.lock();
            try {
                while (this.head == null) {
                    available.await();
                }

                final var element = this.dequeue();

                if (this.head != null) {
                    this.available.signal();
                }

                return element;

            } finally {
                lock.unlock();
            }
        }

        @Override
        public E poll() {
            final var lock = LinkedHydra.this.lock;
            lock.lock();
            try {
                if (this.head == null) {
                    return null;
                }

                final var element = this.dequeue();

                if (this.head != null) {
                    this.available.signal();
                }

                return element;

            } finally {
                lock.unlock();
            }
        }

        @Override
        public void put(final E element) throws InterruptedException {
            final var lock = LinkedHydra.this.lock;
            lock.lock();
            try {
                this.enqueue(element);

                this.available.signal();
            } finally {
                lock.unlock();
            }
        }

        private void enqueue(final E element) {
            final Node<E> newNode = new Node<>();
            newNode.globalNext = null;
            newNode.globalPrev = LinkedHydra.this.tail;
            newNode.localNext = null;
            newNode.element = element;

            if (this.tail == null) {
                this.head = newNode;
            } else {
                this.tail.localNext = newNode;
            }

            if (LinkedHydra.this.tail == null) {
                LinkedHydra.this.head = newNode;
            } else {
                LinkedHydra.this.tail.globalNext = newNode;
            }

            this.tail = newNode;
            LinkedHydra.this.tail = newNode;
        }

        private E dequeue() {
            final var curr = this.head;

            this.head = this.head.localNext;
            if (this.head == null) {
                this.tail = null;
            }

            if (curr.globalPrev != null) {
                curr.globalPrev.globalNext = curr.globalNext;
            } else {
                LinkedHydra.this.head = curr.globalNext;
            }

            if (curr.globalNext != null) {
                curr.globalNext.globalPrev = curr.globalPrev;
            } else {
                LinkedHydra.this.tail = curr.globalPrev;
            }

            final var element = curr.element;
            curr.element = null;
            curr.globalPrev = null;
            curr.globalNext = null;
            curr.localNext = null;

            return element;
        }
    }
}
