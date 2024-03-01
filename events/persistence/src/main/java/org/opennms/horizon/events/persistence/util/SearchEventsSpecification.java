package org.opennms.horizon.events.persistence.util;

import org.opennms.horizon.events.persistence.model.Event;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class SearchEventsSpecification {

    public static Specification<Event> hasEventsUei(Long nodeId,String searchTerm) {
        return (root, query, criteriaBuilder) ->  criteriaBuilder.and(
            criteriaBuilder.like(root.get(Constants.EVENTS_COL_EVENTS_UEI), "%" + searchTerm + "%"),
            criteriaBuilder.equal(root.get(Constants.EVENTS_COL_NODE_ID), nodeId));
    }
    public static Specification<Event> hasLocationName(Long nodeId,String searchTerm) {
        return (root, query, criteriaBuilder) ->  criteriaBuilder.and(
                criteriaBuilder.like(root.get(Constants.EVENTS_COL_LOCATION_NAME), "%" + searchTerm + "%"),
                criteriaBuilder.equal(root.get(Constants.EVENTS_COL_NODE_ID), nodeId));
    }

    public static Specification<Event> hasLogMessage(Long nodeId,String searchTerm) {
        return (root, query, criteriaBuilder) ->  criteriaBuilder.and(
            criteriaBuilder.like(root.get(Constants.EVENTS_COL_LOG_MESSAGE), "%" + searchTerm + "%"),
            criteriaBuilder.equal(root.get(Constants.EVENTS_COL_NODE_ID), nodeId));
    }


    public static Specification<Event> hasDescription(Long nodeId,String searchTerm) {
        return (root, query, criteriaBuilder) ->  criteriaBuilder.and(
            criteriaBuilder.like(root.get(Constants.EVENTS_COL_DESCRIPTION), "%" + searchTerm + "%"),
            criteriaBuilder.equal(root.get(Constants.EVENTS_COL_NODE_ID), nodeId));
    }

     public static Specification<Event> hasIpAddress(Long nodeId,String searchTerm) {
        return (root, query, criteriaBuilder) ->  criteriaBuilder.and(
            criteriaBuilder.equal(root.get(Constants.EVENTS_COL_IPADDRESS), searchTerm),
            criteriaBuilder.equal(root.get(Constants.EVENTS_COL_NODE_ID), nodeId));
    }

}
