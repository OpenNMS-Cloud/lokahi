package org.opennms.horizon.minion.taskset.worker.ignite.resource;

/**
 * Facade to other dependency injection mechanisms and bean registries.
 *
 * Purpose of this type is detachment of actual lookup from ignite specific SPI. Both operations can be called by Ignite
 * itself to initialize fields annotated with @{@link org.apache.ignite.resources.SpringResource}.
 *
 * @author ldywicki
 */
public interface BeanRegistry {

    <T> T lookup(Class<T> type);
    Object lookup(String name);

}
