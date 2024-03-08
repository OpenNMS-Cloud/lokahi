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
package org.opennms.horizon.minion.taskset.worker.ignite.classloader;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opennms.horizon.minion.taskset.worker.ignite.WorkerIgniteConfiguration;

/**
 * Combined class loader which delegates lookup to dynamic list of class loaders which can change over time.
 */
public class CompoundClassLoader extends ClassLoader {

    private final WorkerIgniteConfiguration workerIgniteConfiguration;
    private final List<ClassLoader> loaders;

    public CompoundClassLoader(WorkerIgniteConfiguration workerIgniteConfiguration, List<ClassLoader> loaders) {
        this.workerIgniteConfiguration = workerIgniteConfiguration;
        this.loaders = loaders;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        for (ClassLoader loader : loaders) {
            try {
                return loader.loadClass(name);
            } catch (ClassNotFoundException e) {
                // skip error
            }
        }
        return super.loadClass(name);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        final List<Enumeration<URL>> enums = loaders.stream()
                .map(cl -> {
                    try {
                        return cl.getResources(name);
                    } catch (IOException e) {
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new CompoundEnumeration<>(enums);
    }

    final class CompoundEnumeration<E> implements Enumeration<E> {
        private final List<Enumeration<E>> enums;
        private int index;

        public CompoundEnumeration(List<Enumeration<E>> enums) {
            this.enums = enums;
        }

        private boolean next() {
            while (index < enums.size()) {
                if (enums.get(index) != null && enums.get(index).hasMoreElements()) {
                    return true;
                }
                index++;
            }
            return false;
        }

        public boolean hasMoreElements() {
            return next();
        }

        public E nextElement() {
            if (!next()) {
                throw new NoSuchElementException();
            }
            return enums.get(index).nextElement();
        }
    }
}
