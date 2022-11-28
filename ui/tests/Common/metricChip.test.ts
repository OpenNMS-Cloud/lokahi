import { mount } from '@vue/test-utils'
import { TimeUnit } from '@/types'
import { Chip } from '@/types/metric'
import { getHumanReadableDuration } from '@/components/utils'
import MetricChip from '@/components/Common/MetricChip.vue'

let wrapper: any

describe('Metric chip', () => {
  afterAll(() => {
    wrapper.unmount()
  })

  it.skip('should have `Latency` label, `up` css class and human readable timestamp text', () => {
    const item: Chip = {
      type: 'latency',
      timestamp: -1667930274660,
      timeUnit: TimeUnit.MSecs,
      status: 'UP'
    }

    wrapper = mount(MetricChip, {
      propsData: {
        item
      }
    })

    const label = wrapper.find('label').text()
    expect(label).toEqual(item.type)

    const component = wrapper.findComponent('[data-test="chip"]')
    const text = component.text()
    expect(component.classes().includes(item.status.toLowerCase())).toBe(true)
    // TODO: this test can be failing when running in our build-and-test pipeline (PR)
    // Expected   "7d17h57m54s"
    // Received   "7d17h57m53s"
    expect(text).toEqual(getHumanReadableDuration(item.timestamp as number, item.timeUnit))
  })
  
  it('should have `Status` label, `down` css class and `Down` text', () => {
    const item: Chip = {
      type: 'status',
      status: 'DOWN'
    }

    wrapper = mount(MetricChip, {
      propsData: {
        item
      }
    })

    const label = wrapper.find('label').text()
    expect(label).toEqual(item.type)

    const component = wrapper.findComponent('[data-test="chip"]')
    const text = component.text()
    expect(component.classes().includes(item.status.toLowerCase())).toBe(true)
    expect(text).toEqual(item.status)
  })
})