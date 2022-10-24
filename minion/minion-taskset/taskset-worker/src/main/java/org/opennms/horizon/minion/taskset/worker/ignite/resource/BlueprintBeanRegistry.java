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
