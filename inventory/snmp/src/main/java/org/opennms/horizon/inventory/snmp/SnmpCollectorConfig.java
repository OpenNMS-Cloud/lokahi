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
package org.opennms.horizon.inventory.snmp;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Value;
import org.opennms.horizon.inventory.snmp.config.Configuration;
import org.opennms.horizon.inventory.snmp.config.Group;
import org.opennms.horizon.inventory.snmp.config.MibObj;
import org.opennms.horizon.inventory.snmp.config.ResourceType;
import org.opennms.horizon.inventory.snmp.config.SystemDefChoice;
import org.opennms.horizon.inventory.snmp.part.Part;
import org.opennms.horizon.inventory.snmp.part.ScalarPart;
import org.opennms.horizon.inventory.snmp.part.TablePart;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Value
public class SnmpCollectorConfig {

    List<Configuration> configurations;

    Map<String, Part> groups;

    private SnmpCollectorConfig(final List<Configuration> configurations) {
        this.configurations = Objects.requireNonNull(configurations);

        // Build resource-type table
        final var resourceTypes = this.configurations.stream()
                .flatMap(configuration -> configuration.getResourceTypes().stream())
                .collect(Collectors.toMap(ResourceType::getName, Function.identity()));

        // Build a group lookup table
        this.groups = this.configurations.stream()
                .flatMap(configuration -> configuration.getGroups().stream())
                .map(group -> Map.entry(group.getName(), createPart(group, resourceTypes)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static SnmpCollectorConfig load() throws IOException {
        final var cl = Thread.currentThread().getContextClassLoader();
        final var resolver = new PathMatchingResourcePatternResolver(cl);

        final var resources = resolver.getResources("classpath:configs/snmp/*.xml");

        final Unmarshaller unmarshaller;
        try {
            final var jaxbContext = JAXBContext.newInstance(Configuration.class);
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (final JAXBException e) {
            throw new IOException("Failed to initialize JAXB", e);
        }

        final var configurations = new ArrayList<Configuration>(resources.length);
        for (final var resource : resources) {
            final Configuration configuration;
            try {
                configuration = (Configuration) unmarshaller.unmarshal(resource.getInputStream());
            } catch (final JAXBException e) {
                throw new IOException("Failed to load SNMP config: " + resource.getFilename(), e);
            }

            configurations.add(configuration);
        }

        return new SnmpCollectorConfig(configurations);
    }

    // TODO LOK-2403: factor in ifType
    public Stream<Part> findMatchingParts(final String systemObjId) {
        // Find all matching system-defs in all configurations
        final var systemDefs = this.configurations.stream()
                .flatMap(configuration -> configuration.getSystemDefs().stream()
                        .filter(systemDef -> matchSystemDef(systemDef.getSystemDefChoice(), systemObjId)));

        // Get all groups included by all matching system-defs
        final var groups = systemDefs
                .flatMap(systemDef -> systemDef.getCollect().getIncludeGroups().stream())
                .map(name -> Optional.ofNullable(this.groups.get(name))
                        .orElseThrow(() -> new IllegalStateException("Invalid group reference: " + name)));

        // Merge the table-groups with identical instance names
        final var scalars = new ArrayList<ScalarPart>();
        final var tables = new HashMap<String, TablePart>();
        groups.forEach(group -> {
            group.asScalar().ifPresent(scalars::add);
            group.asTable().ifPresent(part -> tables.merge(part.getInstance(), part, TablePart::merge));
        });

        return Stream.concat(scalars.stream(), tables.values().stream());
    }

    private static Part createPart(final Group group, final Map<String, ResourceType> resourceTypes) {
        // The "instance" of all objects in a group must either positive integer numbers or all must have the same
        // string value
        final var allNumeric = group.getMibObjects().stream()
                .allMatch(obj -> Utils.tryParseInt(obj.getInstance()).isPresent());

        if (allNumeric) {
            // All instances are integers - build a group part where the instances are just appended to the OIDs
            final var elements = group.getMibObjects().stream()
                    .map(obj -> ScalarPart.Element.builder()
                            .oid(String.format("%s.%s", obj.getOid(), obj.getInstance()))
                            .alias(obj.getAlias())
                            .type(obj.getType().toLowerCase())
                            .build())
                    .toList();

            return ScalarPart.builder().elements(elements).build();

        } else {
            // Not all instances are integer - ensure they are all equal and build a table
            assert group.getMibObjects().stream()
                                    .map(MibObj::getInstance)
                                    .distinct()
                                    .count()
                            <= 1
                    : "SNMP collection group with differing instances: " + group.getName();

            final var builder = TablePart.builder();

            // We can assume to have at least one element, otherwise `allNumeric` would have been true
            final var instance = group.getMibObjects().get(0).getInstance();
            builder.instance(instance);

            // Resolve the resource type from current config
            if (instance.equals("ifIndex")) {
                builder.persistFilterExpr(null);
            } else {
                final var resourceType = Optional.ofNullable(resourceTypes.get(instance))
                        .orElseThrow(() -> new IllegalStateException("Missing resource type: " + instance));
                builder.persistFilterExpr(extractPersistFilterExpression(resourceType));
            }

            // Create columns for all objects
            builder.columns(group.getMibObjects().stream()
                    .map(obj -> TablePart.Column.builder()
                            .oid(obj.getOid())
                            .alias(obj.getAlias())
                            .type(obj.getType().toLowerCase())
                            .build())
                    .toList());

            return builder.build();
        }
    }

    private static String extractPersistFilterExpression(final ResourceType resourceType) {
        final var persistenceSelectorStrategy = resourceType.getPersistenceSelectorStrategy();
        return switch (persistenceSelectorStrategy.getClazz()) {
            case "org.opennms.netmgt.collection.support.PersistAllSelectorStrategy" -> null;
            case "org.opennms.netmgt.collectd.PersistRegexSelectorStrategy" -> persistenceSelectorStrategy
                    .findParameterValueByKey("match-expression")
                    .orElse(null);
            default -> throw new IllegalStateException("Unknown resource type");
        };
    }

    private static boolean matchSystemDef(final SystemDefChoice choice, final String needle) {
        if (choice.getSysoid() != null) {
            return needle.equals(choice.getSysoid());
        } else if (choice.getSysoidMask() != null) {
            return needle.startsWith(choice.getSysoidMask());
        } else {
            return false;
        }
    }

    private static class Utils {
        public static Optional<Integer> tryParseInt(final String s) {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (final NumberFormatException e) {
                return Optional.empty();
            }
        }
    }
}
