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
package org.opennms.horizon.minion.taskset.worker.ignite.resource;

import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.blueprint.container.NoSuchComponentException;

/**
 * Realization of {@link BeanRegistry} facade based on OSGi Blueprint API.
 *
 * Standard blueprint API does not provide component (bean) lookup by type, hence it is emulated by facade.
 * Consider this while using this registry as lookup by type might be considered more expensive than lookup by name.
 */
public class BlueprintBeanRegistry implements BeanRegistry {

    private final BlueprintContainer container;

    public BlueprintBeanRegistry(BlueprintContainer container) {
        this.container = container;
    }

    @Override
    public <T> T lookup(Class<T> type) {
        for (String id : container.getComponentIds()) {
            Object bean = container.getComponentInstance(id);
            if (type.isInstance(bean)) {
                return type.cast(bean);
            }
        }
        return null;
    }

    @Override
    public Object lookup(String name) {
        try {
            return container.getComponentInstance(name);
        } catch (NoSuchComponentException e) {
            return null;
        }
    }
}
