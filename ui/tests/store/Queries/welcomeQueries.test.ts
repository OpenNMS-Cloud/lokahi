import { createTestingPinia } from '@pinia/testing'
import { useWelcomeStore } from '@/store/Views/welcomeStore'


describe('Welcome Queries', () => {
    beforeEach(() => {
        createTestingPinia()
    })

    afterEach(() => {
        vi.restoreAllMocks()
    })

    it('fetch welcome locations', async () => {
        vi.mock('villus', () => ({
            useQuery: vi.fn().mockImplementation(() => ({
                data: {
                    value: {
                        getLocationsForWelcome: [{ id: 1, location: 'Welcome' }]
                    }
                }
            }))
        }))

        const welcomeStore = useWelcomeStore()
        await welcomeStore.init();
        expect(welcomeStore.firstLocation).toStrictEqual({ id: -1, location: '' })
    })
})
