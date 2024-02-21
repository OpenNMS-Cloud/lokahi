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
package org.opennms.horizon.minion.flows.parser.factory.dnsresolver;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * An infinite iterator that will return random items
 * from the given list.
 *
 * @param <T> the type of elements in the list
 */
@SuppressWarnings("L61")
public class RandomIterator<T> implements Iterable<T> {
    private final List<T> items;
    private final SecureRandom random;

    public RandomIterator(final List<T> coll) {
        items = new ArrayList<>(coll);
        random = new SecureRandom();
    }

    @SuppressWarnings({"L61", "L62"})
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public synchronized T next() {
                return items.get(random.nextInt(items.size()));
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Cannot remove from " + RandomIterator.class);
            }
        };
    }
}
