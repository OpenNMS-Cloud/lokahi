// import { mount } from '@vue/test-utils'
import mountWithPiniaVillus from 'tests/mountWithPiniaVillus'
import InventoryNodeTagEditOverlay from '@/components/Inventory/InventoryNodeTagEditOverlay.vue'
import { TimeUnit } from '@/types'

const nodeMock = {
  id: 1,
  label: 'Monitored Node 1',
  status: '',
  metrics: [
    {
      type: 'latency',
      label: 'Latency',
      value: 9,
      timeUnit: TimeUnit.MSecs,
      status: 'UP'
    },
    {
      type: 'uptime',
      label: 'Uptime',
      value: 1667930274.66,
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
    tagValue: []
  },
  isNodeOverlayChecked: false
}

let wrapper: any

describe('InventoryNodeTagEditOverlay.vue', () => {
  beforeAll(() => {
    wrapper = mountWithPiniaVillus({
      component: InventoryNodeTagEditOverlay,
      props: { node: nodeMock },
      shallow: true
    })
  })

  test('Mount', () => {
    expect(wrapper).toBeTruthy()
  })

  const requiredElems = [
    ['checkbox', 'tab-node-checkbox'],
    ['heading', 'heading'],
    ['title', 'title-label'],
    ['tag list', 'tag-list']
  ]
  test.each(requiredElems)('should have the "%s" element', (_, dataTest) => {
    const elem = wrapper.get(`[data-test="${dataTest}"]`)
    expect(elem.exists()).toBe(true)
  })
})
