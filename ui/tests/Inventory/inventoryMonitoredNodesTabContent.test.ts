import { mount } from '@vue/test-utils'
import InventoryMonitoredNodesTabContent from '@/components/Inventory/InventoryMonitoredNodesTabContent.vue'
// import InventoryMonitoredNodesTabContent from '../../src/components/Inventory/InventoryMonitoredNodesTabContent.vue'
import { TimeUnit } from '@/types'
import { NodeContent } from '@/types/inventory'
import { useTaggingStore } from '@/store/Components/taggingStore'
import { TagNodesType } from '@/types/tags'
import useModal from '@/composables/useModal'

// const tabContent: NodeContent[] = [
const tabContent: any = [
  {
    id: 1,
    label: 'Monitored Node 1',
    status: '',
    metrics: [
      {
        type: 'latency',
        label: 'Latency',
        timestamp: 9,
        timeUnit: TimeUnit.MSecs,
        status: 'UP'
      },
      {
        type: 'uptime',
        label: 'Uptime',
        timestamp: 1667930274.66,
        timeUnit: TimeUnit.Secs,
        status: 'DOWN'
      },
      {
        type: 'status',
        label: 'Status',
        status: 'DOWN'
      }
    ],
    anchor: {
      profileValue: 75,
      profileLink: 'goto',
      locationValue: 'DefaultMinion',
      locationLink: 'goto',
      managementIpValue: '0.0.0.0',
      managementIpLink: 'goto',
      tagValue: 100,
      tagLink: 'goto'
    },
    isTaggingChecked: false,
    isEditMode: false
  }
]

let wrapper: any

describe('InventoryMonitoredNodesTabContent.vue', () => {
  beforeAll(() => {
    wrapper = mount(InventoryMonitoredNodesTabContent, {
      // shallow: true,
      props: {
        tabContent
      }
    })
  })
  afterAll(() => {
    wrapper.unmount()
  })

  const tabElements = [
    ['Icon', 'icon-storage'],
    ['Heading', 'heading'],
    ['Chip list', 'metric-chip-list'],
    ['Link list', 'text-anchor-list'],
    ['Action list', 'icon-action-list']
  ]
  /* test.each(tabElements)('Should have "%s" element', (elem) => {
    expect(wrapper.get(`[data-test="${elem}"]`).exists()).toBeTruthy()
  }) */

  test('Should have node edit overlay', () => {
    const { isVisible = false } = useModal()
    const taggingStore = useTaggingStore()
    taggingStore.tagNodesSelected = TagNodesType.All

    const modal = wrapper.get('[data-test="primary-model"]')
    expect(modal.exists()).toBeTruthy()
  })
})
