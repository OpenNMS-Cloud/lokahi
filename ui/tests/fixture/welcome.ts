export const defaultNodeDetails = (dateN: number) => ({
    ListNodesForTable: {
        findAllNodes: [
            {
                id: 1,
                nodeLabel: '192.168.1.1',
                tenantId: 'opennms-prime',
                createTime: dateN,
                monitoringLocationId: 1,
                ipInterfaces: [{ ipAddress: '192.168.1.1', snmpPrimary: true }], scanType: 'DISCOVERY_SCAN'
            }]
    },
    ListNodeMetrics: {
        nodeLatency: {
            "status": "success",
            "data": {
                "result": [
                    {
                        "metric": {
                            "instance": "192.168.1.1",
                            "location_id": "2",
                            "monitor": "ICMP",
                            "node_id": "1",
                            "system_id": "default"
                        },
                        "values": [
                            [
                                1687795918.388,
                                7.129
                            ]
                        ]
                    }
                ]
            }
        },
        nodeStatus: {
            "id": 1,
            "status": "UP"
        }
    }
})