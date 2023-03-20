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

package org.opennms.horizon.inventory.service.taskset.monitor;

import com.google.protobuf.Message;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

abstract class SimpleParser<T extends Message> implements ConfigParser<T> {

    private String plugin;

    SimpleParser(String plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getPlugin() {
        return plugin;
    }

    protected final void map(Map<String, String> map, String key, String fallback, Consumer<String> consumer) {
        map(map, key, fallback, Function.identity(), consumer);
    }

    protected final void map(Map<String, String> map, String key, boolean fallback, Consumer<Boolean> consumer) {
        map(map, key, () -> fallback, Boolean::parseBoolean, consumer);
    }

    protected final void map(Map<String, String> map, String key, int fallback, Consumer<Integer> consumer) {
        map(map, key, () -> fallback, Integer::parseInt, consumer);
    }

    protected final void map(Map<String, String> map, String key, Long fallback, Consumer<Long> consumer) {
        map(map, key, () -> fallback, Long::parseLong, consumer);
    }

    protected final <X> void map(Map<String, String> map, String key, String fallback, Function<String, X> mapper, Consumer<X> consumer) {
        map(map, key, () -> mapper.apply(fallback), mapper, consumer);
    }

    protected final <X> void map(Map<String, String> map, String key, Supplier<X> fallback, Function<String, X> mapper, Consumer<X> consumer) {
        if (map.containsKey(key)) {
            String value = map.get(key);
            X mappedValue = mapper.apply(value);
            consumer.accept(mappedValue);
            return;
        }

        consumer.accept(fallback.get());
    }
}
