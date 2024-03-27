import { createTestingPinia } from '@pinia/testing'
import { useInventoryStore } from '../../../src/store/Views/inventoryStore'
import { setActiveClient, useClient } from 'villus'
import {NewInventoryNode, RawMetrics } from '@/types'
import { useInventoryQueries } from '@/store/Queries/inventoryQueries'


describe('Inventory Store', () => {
  beforeEach(() => {
    createTestingPinia({ stubActions: false })
    setActiveClient(useClient({ url: 'http://test/graphql' })) // Create and set a client
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })
  it('Test for toggleTagManager', () => {
    const inventoryStore = useInventoryStore()
    vi.spyOn(inventoryStore, 'toggleTagManager')
    inventoryStore.toggleTagManager()
    expect(inventoryStore.toggleTagManager).toBeCalled()
  })


  it('Test for toggleFilter', () => {
    const inventoryStore = useInventoryStore()
    vi.spyOn(inventoryStore, 'toggleFilter')
    inventoryStore.toggleFilter()
    expect(inventoryStore.toggleFilter).toBeCalled()
  })

  it('Test for toggleNodeEditMode', () => {
    const inventoryStore = useInventoryStore()
    vi.spyOn(inventoryStore, 'toggleNodeEditMode')
    inventoryStore.toggleNodeEditMode()
    expect(inventoryStore.toggleNodeEditMode).toBeCalled()
  })

  it('Test for resetNodeEditMode', () => {
    const inventoryStore = useInventoryStore()
    vi.spyOn(inventoryStore, 'resetNodeEditMode')
    inventoryStore.resetNodeEditMode()
    expect(inventoryStore.resetNodeEditMode).toBeCalled()
  })


  it('Test for addSelectedTag', () => {
    const inventoryStore = useInventoryStore()
    vi.spyOn(inventoryStore, 'addSelectedTag')
    inventoryStore.addSelectedTag([{id: 1, name: 'node1'}])
    expect(inventoryStore.addSelectedTag).toBeCalledWith([{id: 1, name: 'node1'}])
    expect(inventoryStore.tagsSelected).toStrictEqual([{id: 1, name: 'node1'}])
  })



  it('Test for addRemoveNodesSelected', () => {
    const inventoryStore = useInventoryStore()
    const mockNewInventoryNode: NewInventoryNode = {
      id: 1,
      ipInterfaces: [
        {
          id: 1,
          ipAddress: '192.168.1.1',
          nodeId: 1,
          snmpPrimary: true
        }
      ],
      location: {
        id: 1,
        location: 'Location A'
      },
      monitoredState: 'Monitored',
      monitoringLocationId: 1,
      nodeLabel: 'Node 1',
      scanType: 'Scan',
      tags: [
        {
          id: 1,
          name: 'Tag1'
        }

      ],
      nodeAlias: 'Alias'
    }
    vi.spyOn(inventoryStore, 'addRemoveNodesSelected')
    inventoryStore.addRemoveNodesSelected(mockNewInventoryNode)
    expect(inventoryStore.addRemoveNodesSelected).toBeCalled()
    expect(inventoryStore.nodesSelected).toStrictEqual([mockNewInventoryNode])
  })


  it('Test for selectAll', () => {
    const inventoryStore = useInventoryStore()
    const mockNewInventoryNode: NewInventoryNode = {
      id: 1,
      ipInterfaces: [
        {
          id: 1,
          ipAddress: '192.168.1.1',
          nodeId: 1,
          snmpPrimary: true
        }
      ],
      location: {
        id: 1,
        location: 'Location A'
      },
      monitoredState: 'Monitored',
      monitoringLocationId: 1,
      nodeLabel: 'Node 1',
      scanType: 'Scan',
      tags: [
        {
          id: 1,
          name: 'Tag1'
        }

      ],
      nodeAlias: 'Alias'
    }
    vi.spyOn(inventoryStore, 'selectAll')
    inventoryStore.selectAll([mockNewInventoryNode])
    expect(inventoryStore.selectAll).toBeCalled()
    expect(inventoryStore.nodesSelected).toStrictEqual([mockNewInventoryNode])
  })


  it('Test for clearAll', () => {
    const inventoryStore = useInventoryStore()
    const mockNewInventoryNode: NewInventoryNode = {
      id: 1,
      ipInterfaces: [
        {
          id: 1,
          ipAddress: '192.168.1.1',
          nodeId: 1,
          snmpPrimary: true
        }
      ],
      location: {
        id: 1,
        location: 'Location A'
      },
      monitoredState: 'Monitored',
      monitoringLocationId: 1,
      nodeLabel: 'Node 1',
      scanType: 'Scan',
      tags: [
        {
          id: 1,
          name: 'Tag1'
        }

      ],
      nodeAlias: 'Alias'
    }
    inventoryStore.selectAll([mockNewInventoryNode])
    vi.spyOn(inventoryStore, 'clearAll')
    expect(inventoryStore.nodesSelected).toStrictEqual([mockNewInventoryNode])
    inventoryStore.clearAll()
    expect(inventoryStore.clearAll).toBeCalled()
    expect(inventoryStore.nodesSelected).toStrictEqual([])
  })

  it('Test for setSearchType', () => {
    const inventoryStore = useInventoryStore()
    vi.spyOn(inventoryStore, 'setSearchType')
    inventoryStore.setSearchType({id: 1, name: 'node1'})
    expect(inventoryStore.setSearchType).toBeCalledWith({id: 1, name: 'node1'})
    expect(inventoryStore.searchType).toStrictEqual({id: 1, name: 'node1'})
  })

  it('Test for resetSelectedNode', () => {
    const inventoryStore = useInventoryStore()
    vi.spyOn(inventoryStore, 'resetSelectedNode')
    inventoryStore.resetSelectedNode()
    expect(inventoryStore.resetSelectedNode).toBeCalled()
    expect(inventoryStore.nodesSelected).toStrictEqual([])
  })



  it('Test for resetSelectedNode', () => {
    const inventoryStore = useInventoryStore()
    vi.spyOn(inventoryStore, 'resetSelectedNode')
    inventoryStore.resetSelectedNode()
    expect(inventoryStore.resetSelectedNode).toBeCalled()
    expect(inventoryStore.nodesSelected).toStrictEqual([])
  })


  it('Test for init', () => {
    const inventoryStore = useInventoryStore()
    vi.spyOn(inventoryStore, 'init')
    inventoryStore.init()
    expect(inventoryStore.init).toBeCalled()
  })

  it('Test for getNodesByTags', async () => {
    const query = useInventoryQueries()
    vi.spyOn(query, 'getNodesByTags')
    await query.getNodesByTags('Tag1')
    expect(query.getNodesByTags).toBeCalled()
  })


  it('Test for getNodesByLabel', async () => {
    const query = useInventoryQueries()
    vi.spyOn(query, 'getNodesByLabel')
    await query.getNodesByLabel('Tag1')
    expect(query.getNodesByLabel).toBeCalledWith('Tag1')
  })




  it('Test for receivedNetworkInventory ', () => {
    const store = useInventoryStore()
    const mockNewInventoryNode: NewInventoryNode = {
      id: 1,
      ipInterfaces: [
        {
          id: 1,
          ipAddress: '192.168.1.1',
          nodeId: 1,
          snmpPrimary: true
        }
      ],
      location: {
        id: 1,
        location: 'Location A'
      },
      monitoredState: 'Monitored',
      monitoringLocationId: 1,
      nodeLabel: 'Node 1',
      scanType: 'Scan',
      tags: [
        {
          id: 1,
          name: 'Tag1'
        }

      ],
      nodeAlias: 'Alias'
    }
    const allMetrics: RawMetrics =  {
      status: 'ACTIVE',
      data: {
        resultType: '',
        result: [{
          metric: {
            __name__: '',
            instance: '',
            location_id: '1',
            monitor: 'ACTIVE',
            node_id: '',
            system_id: 'string'
          },
          value: [1, 2],
          values: []
        }]
      }
    }
    const testData = {
      findAllNodes: [mockNewInventoryNode],
      allMetrics
    }
    vi.spyOn(store, 'receivedNetworkInventory')
    store.receivedNetworkInventory(testData)
    expect(store.receivedNetworkInventory).toBeCalledWith(testData)
  })
})


