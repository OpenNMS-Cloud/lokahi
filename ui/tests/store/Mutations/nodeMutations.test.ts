import { createTestingPinia } from '@pinia/testing'
import { nodeFixture } from '../../fixture/nodes'

describe('nodeMutations', () => {
  beforeEach(() => {
    createTestingPinia()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  // TODO: look for how to test mutations
  it('should add a node', async () => {
    // node list
    vi.mock('villus', () => ({
      useQuery: vi.fn().mockImplementation(() => ({
        data: {
          value: {
            findAllNodes: [nodeFixture()]
          }
        },
        isFetching: {
          value: false
        }
      }))
    }))

    // add a node
    // const nodeMutations = useNodeMutations()
    // nodeMutations.addNode()
    // assert: node list + 1 element
  })

  // TODO: look for how to test mutations
  it('should remove a node', () => {
    // node list
    // delete a node
    // assert: node list - 1 element
  })
})
