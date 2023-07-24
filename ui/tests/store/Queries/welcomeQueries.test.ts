import { useWelcomeStore } from '@/store/Views/welcomeStore'
import { setActiveClient, useClient } from 'villus'
import { buildFetchList } from 'tests/utils'
import { createTestingPinia } from '@pinia/testing'
import { useWelcomeQueries } from '@/store/Queries/welcomeQueries'
import { defaultNodeDetails } from 'tests/fixture/welcome'


describe('Welcome Queries', () => {
    beforeEach(() => {
        createTestingPinia({ stubActions: false })
    })

    afterEach(() => {
        vi.restoreAllMocks()
    })

    it('perform store init', async () => {

        global.fetch = buildFetchList({
            FindLocationsForWelcome: { findAllLocations: [{ id: 1, location: 'default' }] },
            ListMinionsForTable: { findAllMinions: [{ id: 1, label: 'DEFAULT', lastCheckedTime: Date.now(), location: { id: 2, location: 'default' } }] }
        })
        setActiveClient(useClient({ url: 'http://test/graphql' }))

        const welcomeStore = useWelcomeStore()
        await welcomeStore.init();
        expect(welcomeStore.firstLocation).toStrictEqual({ id: -1, location: '' })
    })

    it('get all welcome minions', async () => {

        const dateN = Date.now()
        global.fetch = buildFetchList({
            ListMinionsForTable: { findAllMinions: [{ id: 1, label: 'DEFAULT', lastCheckedTime: dateN, location: { id: 2, location: 'default' } }] }
        })
        setActiveClient(useClient({ url: 'http://test/graphql' }))

        const welcomeStore = useWelcomeQueries()
        const minions = await welcomeStore.getAllMinions();
        expect(minions).toStrictEqual([{ id: 1, label: 'DEFAULT', lastCheckedTime: dateN, location: { id: 2, location: 'default' } }])
    })

    it('get all welcome locations', async () => {
        const dateN = Date.now()
        global.fetch = buildFetchList({
            FindLocationsForWelcome: { findAllLocations: [{ id: 1, location: 'default' }] },
        })
        setActiveClient(useClient({ url: 'http://test/graphql' }))

        const welcomeStore = useWelcomeQueries()
        const locations = await welcomeStore.getLocationsForWelcome();
        expect(locations).toStrictEqual([{ id: 1, location: 'default' }])
    })

    it('get welcome minion certificate', async () => {
        global.fetch = buildFetchList({
            getMinionCertificate: { getMinionCertificate: { password: 'tempPassword', certificate: '234234098098' } },
        })
        setActiveClient(useClient({ url: 'http://test/graphql' }))

        const welcomeStore = useWelcomeQueries()
        const minionCert = await welcomeStore.getMinionCertificate(1)
        expect(minionCert).toStrictEqual({ password: 'tempPassword', certificate: '234234098098' })
    })

    it('get welcome node details', async () => {
        const dateN = Date.now()
        global.fetch = buildFetchList({
            ...defaultNodeDetails(dateN)
        })

        setActiveClient(useClient({ url: 'http://test/graphql' }))

        const welcomeStore = useWelcomeQueries()
        const minionCert = await welcomeStore.getNodeDetails('default');
        expect(minionCert).toStrictEqual({
            detail: {
                id: 1,
                nodeLabel: '192.168.1.1',
                tenantId: 'opennms-prime',
                createTime: dateN,
                monitoringLocationId: 1,
                ipInterfaces: [{ ipAddress: '192.168.1.1', snmpPrimary: true }], scanType: 'DISCOVERY_SCAN'
            },
            metrics: {
                nodeLatency: {
                    status: 'success', data: {
                        result: [{
                            metric: {
                                instance: '192.168.1.1',
                                location_id: '2',
                                monitor: 'ICMP',
                                node_id: '1',
                                system_id: 'default'
                            }, values: [
                                [1687795918.388, 7.129]
                            ]
                        }]
                    }
                },
                nodeStatus: { id: 1, status: 'UP' }
            }
        })
    })
})
