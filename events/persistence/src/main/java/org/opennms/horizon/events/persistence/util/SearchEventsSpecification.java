package org.opennms.horizon.events.persistence.util;

import org.opennms.horizon.events.persistence.model.Event;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class SearchEventsSpecification {

    public static Specification<Event> hasEventsUei(String tenantId,Long nodeId,String searchTerm) {
        return (root, query, criteriaBuilder) ->  criteriaBuilder.and(
            criteriaBuilder.like(root.get(Constants.EVENTS_COL_EVENTS_UEI), "%" + searchTerm + "%"),
            criteriaBuilder.equal(root.get(Constants.EVENTS_COL_NODE_ID), nodeId),
            criteriaBuilder.equal(root.get(Constants.EVENTS_COL_TENANT_ID), tenantId));
    }
    public static Specification<Event> hasLocationName(String tenantId,Long nodeId,String searchTerm) {
        return (root, query, criteriaBuilder) ->  criteriaBuilder.and(
                criteriaBuilder.like(root.get(Constants.EVENTS_COL_LOCATION_NAME), "%" + searchTerm + "%"),
                criteriaBuilder.equal(root.get(Constants.EVENTS_COL_NODE_ID), nodeId),
                criteriaBuilder.equal(root.get(Constants.EVENTS_COL_TENANT_ID), tenantId));
    }

    public static Specification<Event> hasLogMessage(String tenantId,Long nodeId,String searchTerm) {
        return (root, query, criteriaBuilder) ->  criteriaBuilder.and(
            criteriaBuilder.like(root.get(Constants.EVENTS_COL_LOG_MESSAGE), "%" + searchTerm + "%"),
            criteriaBuilder.equal(root.get(Constants.EVENTS_COL_NODE_ID), nodeId),
            criteriaBuilder.equal(root.get(Constants.EVENTS_COL_TENANT_ID), tenantId));
    }


    public static Specification<Event> hasDescription(String tenantId,Long nodeId,String searchTerm) {
        return (root, query, criteriaBuilder) ->  criteriaBuilder.and(
            criteriaBuilder.like(root.get(Constants.EVENTS_COL_DESCRIPTION), "%" + searchTerm + "%"),
            criteriaBuilder.equal(root.get(Constants.EVENTS_COL_NODE_ID), nodeId),
            criteriaBuilder.equal(root.get(Constants.EVENTS_COL_TENANT_ID), tenantId));
    }

     public static Specification<Event> hasIpAddress(String tenantId,Long nodeId,String searchTerm) {
        return (root, query, criteriaBuilder) ->  criteriaBuilder.and(
            criteriaBuilder.equal(root.get(Constants.EVENTS_COL_IPADDRESS), InetAddressUtils.addr(searchTerm)),
            criteriaBuilder.equal(root.get(Constants.EVENTS_COL_NODE_ID), nodeId),
            criteriaBuilder.equal(root.get(Constants.EVENTS_COL_TENANT_ID), tenantId));
    }

}
