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
package org.opennms.miniongateway.ignite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryUpdatedListener;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.junit.Test;

/**
 * DEVELOPER TEST - only executed manually
 *
 * Tests to verify ignite operations
 */
public class IgniteDT {

    @Test
    public void testIgnite() {
        testIgniteLock();

        testIgniteCacheEntryMutation(true, true);
        testIgniteCacheEntryMutation(false, false);

        testIgniteCacheEntryMutation(false, true);
        testIgniteCacheEntryMutation(true, false);
    }

    /**
     * Verify Ignite cache locking
     */
    private void testIgniteLock() {
        Ignite ignite = Ignition.start();

        try {
            var config = prepareCacheConfiguration(true, true);
            IgniteCache<String, CacheTestObject> cache = ignite.getOrCreateCache(config);

            // Can we lock an entry that is not (yet) in the cache?
            Lock lock = cache.lock("item001");
            lock.lock();

            Thread thread = new Thread(() -> {
                System.out.println("SECOND LOCK");
                Lock secondLock = cache.lock("item001");
                secondLock.lock();
                System.out.println("SECOND LOCK COMPLETE");

                CacheTestObject entry2 = createTestObject();
                cache.put("item001", entry2);

                secondLock.unlock();
            });

            thread.setDaemon(true);
            thread.start();
            thread.join(3000);

            if (thread.isAlive()) {
                System.out.println("BLOCKED");
            } else {
                System.out.println("NOT BLOCKED");
            }

            CacheTestObject entry1 = createTestObject();
            cache.put("item001", entry1);
        } catch (Exception exc) {
            exc.printStackTrace();
        }

        ignite.close();
    }

    private void testIgniteCacheEntryMutation(boolean onHeapCache, boolean copyOnRead) {
        System.out.println("vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
        System.out.println("========================================");
        System.out.println("ON-HEAP-CACHE = " + onHeapCache);
        System.out.println("COPY-ON-READ  = " + copyOnRead);
        System.out.println("========================================");

        Ignite ignite = Ignition.start();

        try {
            var config = prepareCacheConfiguration(onHeapCache, copyOnRead);
            IgniteCache<String, CacheTestObject> cache = ignite.getOrCreateCache(config);

            CacheTestObject entry1 = createTestObject();

            cache.registerCacheEntryListener(new MutableCacheEntryListenerConfiguration<String, CacheTestObject>(
                    new MyCacheEntryListenerFactory(), null, false, false));

            //
            // INITIAL PUT
            //
            System.out.println("ADDING item001 with id=" + System.identityHashCode(entry1));
            cache.put("item001", entry1);

            // GET 1
            CacheTestObject entry1B = cache.get("item001");
            System.out.println("RETURNED item001 with id=" + System.identityHashCode(entry1B) + "; "
                    + formatIsSame(entry1, entry1B));

            // UPDATE THE ORIGINALLY "put" OBJECT
            entry1.subObjects.add(new CacheTestSubObject("item001.sub003-id"));

            // DUMP AND ADD TO RETURNED VALUE, DUMP AGAIN
            System.out.println("ENTRY SUB-OBJ LIST AFTER GET:");
            System.out.print("    ");
            dumpJson(entry1B.subObjects);
            entry1B.subObjects.add(new CacheTestSubObject("item001.sub004-id"));

            System.out.println("ENTRY SUB-OBJ LIST AFTER GET THEN ADD:");
            System.out.print("    ");
            dumpJson(entry1B.subObjects);

            // GET 2
            CacheTestObject entry1C = cache.get("item001");
            System.out.println("RETURNED ANOTHER item001 with id=" + System.identityHashCode(entry1C) + "; "
                    + formatIsSame(entry1B, entry1C));

            // DUMP
            System.out.println("ENTRY SUB-OBJ LIST AFTER 2ND GET:");
            System.out.print("    ");
            dumpJson(entry1C.subObjects);

            //
            // PUT after update
            //
            System.out.println("PUTTING UPDATED ENTRY");
            cache.put("item001", entry1B);

            CacheTestObject entry1D = cache.get("item001");
            System.out.println("RETURNED ANOTHER item001 AFTER UPDATE with id=" + System.identityHashCode(entry1D)
                    + "; " + formatIsSame(entry1B, entry1D));

            System.out.println("ENTRY SUB-OBJ LIST AFTER 3RD GET:");
            System.out.print("    ");
            dumpJson(entry1D.subObjects);

            CacheTestObject entry1E = cache.get("item001");
            System.out.println("SECOND GET AFTER UPDATE with id=" + System.identityHashCode(entry1D) + "; "
                    + formatIsSame(entry1E, entry1D));
        } catch (Exception exc) {
            exc.printStackTrace();
        } finally {
            ignite.close();

            System.out.println("^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
            System.out.println();
        }
    }

    private CacheConfiguration<String, CacheTestObject> prepareCacheConfiguration(
            boolean onHeapCache, boolean copyOnRead) {
        CacheConfiguration<String, CacheTestObject> result = new CacheConfiguration<>("test-cache");

        result.setOnheapCacheEnabled(onHeapCache);
        result.setCopyOnRead(copyOnRead);

        result.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

        // result.setReadThrough(true);
        // result.setWriteThrough(true);

        return result;
    }

    private CacheTestObject createTestObject() {
        return new CacheTestObject(
                "item001-id",
                new LinkedList<>(Arrays.asList(
                        new CacheTestSubObject("item001.sub001-id"), new CacheTestSubObject("item001.sub002-id"))));
    }

    private void dumpJson(Object obj) throws JsonProcessingException {
        System.out.println(new ObjectMapper().writeValueAsString(obj));
    }

    private String formatIsSame(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return "SAME";
        } else {
            return "DISTINCT";
        }
    }

    private static class CacheTestObject implements Serializable {
        public String id;
        public List<CacheTestSubObject> subObjects;

        public CacheTestObject(String id, List<CacheTestSubObject> subObjects) {
            this.id = id;
            this.subObjects = subObjects;
        }
    }

    private class CacheTestSubObject implements Serializable {
        public String id;

        public CacheTestSubObject(String id) {
            this.id = id;
        }
    }

    private class MyCacheEntryListener implements CacheEntryUpdatedListener<String, CacheTestObject> {
        @Override
        public void onUpdated(Iterable<CacheEntryEvent<? extends String, ? extends CacheTestObject>> cacheEntryEvents)
                throws CacheEntryListenerException {
            CacheTestObject cacheTestObject = cacheEntryEvents.iterator().next().getValue();

            try {
                System.out.println(">>> LISTENER - have update: id=" + System.identityHashCode(cacheTestObject));
                System.out.print(">>> ");
                dumpJson(cacheTestObject);
            } catch (Exception exc) {
                exc.printStackTrace();
            }
        }
    }

    private class MyCacheEntryListenerFactory implements Factory<CacheEntryListener<String, CacheTestObject>> {
        @Override
        public CacheEntryListener<String, CacheTestObject> create() {
            return new MyCacheEntryListener();
        }
    }
}
