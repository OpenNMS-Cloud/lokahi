import { useWelcomeStore } from '@/store/Views/welcomeStore'
import { setActiveClient, useClient } from 'villus'
import { buildFetchList } from 'tests/utils'
import { createTestingPinia } from '@pinia/testing'


describe('Welcome Store', () => {
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

    it('can build an item status ', async () => {

        const welcomeStore = useWelcomeStore()
        const itemStatus = welcomeStore.buildItemStatus({ title: 'Hello', condition: true, status: 'ok' })
        expect(itemStatus).toStrictEqual({ title: 'Hello', status: 'ok', statusColor: 'var(--feather-success)', statusText: 'var(--feather-primary-text-on-color)' })
    })

    it('can create a default location', async () => {

        global.fetch = buildFetchList({
            FindLocationsForWelcome: { findAllLocations: [{ id: 1, location: 'default' }] },
        })
        setActiveClient(useClient({ url: 'http://test/graphql' }))

        const welcomeStore = useWelcomeStore()
        await welcomeStore.createDefaultLocation();
        expect(welcomeStore.firstLocation).toStrictEqual({ id: 1, location: 'default' })
    })

    it('can get the docker command', async () => {
        const welcomeStore = useWelcomeStore()
        const theDockerCmd = welcomeStore.dockerCmd()
        expect(theDockerCmd).toBe(`docker run --rm -p 162:1162/udp -p 9999:9999/udp -e USE_KUBERNETES="false" -e MINION_GATEWAY_HOST="minion.onms-fb-prod.production.prod.dataservice.opennms.com" -e MINION_GATEWAY_PORT=443 -e MINION_GATEWAY_TLS="true" -e GRPC_CLIENT_KEYSTORE='/opt/karaf/minion.p12' -e GRPC_CLIENT_KEYSTORE_PASSWORD='' -e MINION_ID='default' --mount type=bind,source="/PATH_TO_DOWNLOADED_FILE/default-certificate.p12",target="/opt/karaf/minion.p12",readonly opennms/lokahi-minion:latest`)
    })

    it('can click on the download button', async () => {
        global.fetch = buildFetchList({
            getMinionCertificate: { getMinionCertificate: { password: 'tempPassword', certificate: '234234098098' } },
            ListMinionsForTable: { findAllMinions: [{ id: 1, label: 'DEFAULT', lastCheckedTime: Date.now(), location: { id: 2, location: 'default' } }] }
        })
        global.window.atob = (data: string) => {
            return '' as any
        }
        global.URL.createObjectURL = (o: any) => {
            return 'http://'
        }
        const welcomeStore = useWelcomeStore()
        await welcomeStore.downloadClick();
        expect(welcomeStore.minionStatusStarted).toBeTruthy()
    })

    it('can get the failure status', () => {

        const welcomeStore = useWelcomeStore()
        const status = welcomeStore.getFailureStatus()
        expect(status).toEqual({
            statusColor: 'var(--feather-error)',
            statusText: 'var(--feather-primary-text-on-color)'
        })
    })

    it('can get the failure status', () => {
        const welcomeStore = useWelcomeStore()
        const status = welcomeStore.getFailureStatus()
        expect(status).toEqual({
            statusColor: 'var(--feather-error)',
            statusText: 'var(--feather-primary-text-on-color)'
        })
    })

    it('can get the success status', () => {
        const welcomeStore = useWelcomeStore()
        const status = welcomeStore.getSuccessStatus()
        expect(status).toEqual({
            statusColor: 'var(--feather-success)',
            statusText: 'var(--feather-primary-text-on-color)'
        })
    })

    it('can make the device preview start loading', () => {
        const welcomeStore = useWelcomeStore()
        welcomeStore.loadDevicePreview()
        expect(welcomeStore.devicePreview.loading).toEqual(true)
    })

    it('can advance slides', () => {
        const welcomeStore = useWelcomeStore()
        welcomeStore.nextSlide();
        expect(welcomeStore.slide).toBe(2)
    })

    it('can to the previous slide', () => {
        const welcomeStore = useWelcomeStore()
        welcomeStore.nextSlide();
        welcomeStore.prevSlide();
        expect(welcomeStore.slide).toBe(1);
    })

    it('can set the device preview', () => {
        const welcomeStore = useWelcomeStore()
        const ip = '192.168.1.1'
        welcomeStore.setDevicePreview({ nodeLabel: ip, createTime: 1234 }, {}, 25)
        expect(welcomeStore.devicePreview.title).toBe('Node Discovery');
        expect(welcomeStore.devicePreview.itemTitle).toBe(ip);
    })

    it('can toggle a slide collapse', () => {
        const welcomeStore = useWelcomeStore()
        welcomeStore.toggleSlideOneCollapse();
        expect(welcomeStore.slideOneCollapseVisible).toBe(false)
    })

    it('can validate on keyup', async () => {
        const welcomeStore = useWelcomeStore()
        welcomeStore.validateOnKeyup = true;
        await welcomeStore.updateFirstDiscovery('name', '');
        expect(welcomeStore.firstDiscoveryErrors.name).toBe('')
    })

    it('can update minion status copy', async () => {
        const welcomeStore = useWelcomeStore()
        welcomeStore.updateMinionStatusCopy();
        expect(welcomeStore.minionStatusCopy).toBe('Waiting for the Docker Install Command to be complete.')
    })
})