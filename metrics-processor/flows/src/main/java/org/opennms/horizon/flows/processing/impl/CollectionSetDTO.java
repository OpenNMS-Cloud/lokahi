/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2017-2017 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2017 The OpenNMS Group, Inc.
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

package org.opennms.horizon.flows.processing.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;


public class CollectionSetDTO implements CollectionSet {

    private CollectionAgentDTO agent;

    private CollectionStatus status = CollectionStatus.SUCCEEDED;

    private Date timestamp;

    private List<CollectionResourceDTO> collectionResources = new ArrayList<>(0);

    private Boolean disableCounterPersistence;

    private Long sequenceNumber;

    public CollectionSetDTO() { }

    public CollectionSetDTO(CollectionAgent agent, CollectionStatus status,
                            Date timestamp, Map<Resource, List<Attribute<?>>> attributesByResource,
                            boolean disableCounterPersistence, Long sequenceNumber) {
        this.agent = new CollectionAgentDTO(agent);
        this.status = status;
        this.timestamp = timestamp;
        this.sequenceNumber = sequenceNumber;
        collectionResources = new ArrayList<>();
        for (Entry<Resource, List<Attribute<?>>> entry : attributesByResource.entrySet()) {
            collectionResources.add(new CollectionResourceDTO(entry.getKey(), entry.getValue()));
        }
        if (disableCounterPersistence) {
            this.disableCounterPersistence = disableCounterPersistence;
        }
    }

    @Override
    public String toString() {
        return String.format("CollectionSetDTO[agent=%s, collectionResources=%s, status=%s, timestamp=%s, disableCounterPersistence=%s]",
                agent, collectionResources, status, timestamp, disableCounterPersistence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(agent, collectionResources, status, timestamp, disableCounterPersistence, sequenceNumber);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof CollectionSetDTO)) {
            return false;
        }
        CollectionSetDTO other = (CollectionSetDTO) obj;
        return Objects.equals(this.agent, other.agent)
               && Objects.equals(this.collectionResources, other.collectionResources)
               && Objects.equals(this.status, other.status)
               && Objects.equals(this.timestamp, other.timestamp)
               && Objects.equals(this.disableCounterPersistence, other.disableCounterPersistence)
               && Objects.equals(this.sequenceNumber, other.sequenceNumber);
    }

    @Override
    public CollectionStatus getStatus() {
        return status;
    }

    @Override
    public boolean ignorePersist() {
        return false;
    }

    @Override
    public Date getCollectionTimestamp() {
        return timestamp;
    }

    public void setCollectionAgent(CollectionAgentDTO agent) {
        this.agent = agent;
    }

    private Set<CollectionResource> buildCollectionResources() {
        final Set<CollectionResource> collectionResources = new LinkedHashSet<>();
        for (CollectionResourceDTO entry : this.collectionResources) {
            final Resource resource = entry.getResource();
            final AbstractCollectionResource collectionResource = CollectionSetBuilder.toCollectionResource(resource, agent);
            for (Attribute<?> attribute : entry.getAttributes()) {
                final AttributeGroupType groupType = new AttributeGroupType(attribute.getGroup(), AttributeGroupType.IF_TYPE_ALL);
                final AbstractCollectionAttributeType attributeType = new AbstractCollectionAttributeType(groupType) {
                    @Override
                    public AttributeType getType() {
                        return attribute.getType();
                    }

                    @Override
                    public String getName() {
                        return attribute.getName();
                    }

                    @Override
                    public void storeAttribute(CollectionAttribute collectionAttribute, Persister persister) {
                        if (AttributeType.STRING.equals(attribute.getType())) {
                            persister.persistStringAttribute(collectionAttribute);
                        } else {
                            persister.persistNumericAttribute(collectionAttribute);
                        }
                    }

                    @Override
                    public String toString() {
                        return attribute.toString();
                    }
                };

                collectionResource.addAttribute(new AbstractCollectionAttribute(attributeType, collectionResource) {
                    @Override
                    public String getMetricIdentifier() {
                        return attribute.getIdentifier() != null ? attribute.getIdentifier() : attribute.getName();
                    }

                    @Override
                    public Number getNumericValue() {
                        return attribute.getNumericValue();
                    }

                    @Override
                    public String getStringValue() {
                        return attribute.getStringValue();
                    }

                    @Override
                    public String toString() {
                        return String.format("Attribute[%s:%s]", getMetricIdentifier(), attribute.getValue());
                    }
                });
            }
            collectionResources.add(collectionResource);
        }
        return collectionResources;
    }

    @Override
    public void visit(CollectionSetVisitor visitor) {
        visitor.visitCollectionSet(this);

        for(CollectionResource resource : buildCollectionResources()) {
            resource.visit(visitor);
        }

        visitor.completeCollectionSet(this);
    }

    @Override
    public OptionalLong getSequenceNumber() {
        return sequenceNumber == null ? OptionalLong.empty() : OptionalLong.of(sequenceNumber);
    }
}
