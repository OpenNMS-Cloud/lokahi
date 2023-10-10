import { createTestingPinia } from '@pinia/testing'
import { minionFixture } from '../../fixture/minions'

describe('minionMutations', () => {
  beforeEach(() => {
    createTestingPinia()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // TODO: look for how to test mutations
  it('should remove a minion', async () => {
    // minion list
    vi.mock('villus', () => ({
      useQuery: vi.fn().mockImplementation(() => ({
        data: {
          value: {
            findAllMinions: [minionFixture()]
          }
        },
        isFetching: {
          value: false
        }
      }))
    }))

    // delete a minion
    // const minionsMutations = useMinionMutations()
    // minionMutations.deleteMinion()
    // assert: minion list - 1 element
  })
})
