package org.opennms.horizon.it;

public abstract class GQLQueryConstants {
    public static final String LIST_MINIONS_QUERY = "{ \"query\": \"{ findAllMinions {id, systemId, systemId, location { location } } }\" }";

    public static final String GET_LABELED_METRICS_QUERY =
        "query { metric(name:\"%s\", labels: {%s:\"%s\"}) { status, data { result { metric, value }}} }";

    public static final String CREATE_NODE_QUERY =
        "mutation AddNode($node: NodeCreateInput!) { addNode(node: $node) { nodeType, details }}";

    public static final String LIST_NODE_METRICS =
        "query NodeStatusParts($id: Long!) {nodeStatus(id: $id) {id status  }}";

    public static final String GET_NODE_ID =
        "query NodesTableParts { findAllNodes { nodeType, details }}";

    public static final String DELETE_NODE_BY_ID =
        "mutation DeleteNode($id: Long!) {  deleteNode(id: $id)}";

    public static final String ADD_DISCOVERY_QUERY =
        "mutation { createIcmpActiveDiscovery( request: { name: \"%s\", location: \"%s\", ipAddresses: [\"%s\"], snmpConfig: { readCommunities: [\"%s\"], ports: [%d]\n" +
            " } } ) {id, name, ipAddresses, location, snmpConfig { readCommunities, ports } } }";

}
