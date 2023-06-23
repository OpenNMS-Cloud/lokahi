package org.opennms.horizon.it;

public abstract class GQLQueryConstants {
    public static final String CREATE_LOCATION = "mutation createLocation($location: String) { createLocation(location: {location: $location}) { id, location } }";

    public static final String DELETE_LOCATION = "mutation deleteLocation($id: Long!) { deleteLocation(id: $id) }";

    public static final String LIST_LOCATIONS_QUERY = "query { findAllLocations { id, location } }";

    public static final String LIST_MINIONS_QUERY = "query { findAllMinions {id, label, systemId, status, location { location } } }";

    public static final String GET_LABELED_METRICS_QUERY =
        "query { metric(name:\"%s\", labels: {%s:\"%s\"}) { status, data { result { metric, value }}} }";

    public static final String LIST_MINION_INSTANCE_ECHO_METRICS_QUERY =
        "query { metric(name:\"response_time_msec\", labels: {monitor:\"ECHO\", instance:\"%s\"}) { status, data { result { metric }}} }";

    public static final String CREATE_NODE_QUERY =
        "mutation AddNode($node: NodeCreateInput!) { addNode(node: $node) { createTime id monitoringLocationId nodeLabel tenantId }}";

    public static final String LIST_NODE_METRICS =
        "query NodeStatusParts($id: Long!) {nodeStatus(id: $id) {id status  }}";

    public static final String GET_NODE_ID =
        "query NodesTableParts { findAllNodes { id nodeLabel}}";

    public static final String DELETE_NODE_BY_ID =
        "mutation DeleteNode($id: Long!) {  deleteNode(id: $id)}";

    public static final String ADD_DISCOVERY_QUERY =
        "mutation { createIcmpActiveDiscovery( request: { name: \"%s\", locationId: \"%s\", ipAddresses: [\"%s\"], snmpConfig: { readCommunities: [\"%s\"], ports: [%d]\n" +
            " } } ) {id, name, ipAddresses, locationId, snmpConfig { readCommunities, ports } } }";

    public static final String CREATE_MINION_CERTIFICATE =
        "query { getMinionCertificate(locationId: \"%d\") {  certificate, password } }";

}
