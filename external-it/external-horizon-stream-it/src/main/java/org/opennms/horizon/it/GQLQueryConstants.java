package org.opennms.horizon.it;

public abstract class GQLQueryConstants {
    public static final String LIST_MINIONS_QUERY = "{ \"query\": \"{ findAllMinions {id, systemId, systemId, location { location } } }\" }";

    public static final String GET_LABELED_METRICS_QUERY =
        "query { metric(name:\"%s\", labels: {%s:\"%s\"}) { status, data { result { metric, value }}} }";

    public static final String LIST_MINION_INSTANCE_ECHO_METRICS_QUERY =
        "query { metric(name:\"response_time_msec\", labels: {monitor:\"ECHO\", instance:\"%s\"}) { status, data { result { metric }}} }";

    public static final String CREATE_NODE_QUERY =
        "mutation AddNode($node: NodeCreateInput!) { addNode(node: $node) { createTime id monitoringLocationId nodeLabel tenantId }}";
}
