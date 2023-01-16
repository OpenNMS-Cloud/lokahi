import { mount } from '@vue/test-utils'
import MetricChip from '@/components/Common/MetricChip.vue'
import { TimeUnit } from '@/types'
import { Chip } from '@/types/metric'
import { getHumanReadableDuration } from '@/components/utils'

let wrapper: any

describe('Metric chip', () => {
  afterAll(() => {
    wrapper.unmount()
  })

  it.skip('should have `Latency` label, `up` css class and human readable latency metric value', () => {
    const metric: Chip = {
      type: 'latency',
      label: 'Latency',
      value: -1667930274660,
      timeUnit: TimeUnit.MSecs,
      status: 'UP'
    }

    wrapper = mount(MetricChip, {
      props: {
        metric
      }
    })

    const label = wrapper.get('label').text()
    expect(label).toEqual(metric.type)

    const component = wrapper.getComponent('[data-test="chip"]')
    const text = component.text()
    expect(component.classes().includes(metric.status?.toLowerCase())).toBe(true)
    // TODO: this test may fail when running in our build-and-test pipeline (PR) - inconsistence
    // Expected   "7d17h57m54s"
    // Received   "7d17h57m53s"
    expect(text).toEqual(getHumanReadableDuration(metric.value as number, metric.timeUnit))
  })

  it('should have `Status` label, `down` css class and `Down` text', () => {
    const metric: Chip = {
      type: 'status',
      label: 'Status',
      status: 'DOWN'
    }

    wrapper = mount(MetricChip, {
      props: {
        metric
      }
    })

    const label = wrapper.get('label').text()
    expect(label).toEqual(metric.label)

    const component = wrapper.getComponent('[data-test="chip"]')
    expect(component.classes().includes(metric.status)).toBe(true)
    expect(component.text()).toEqual(metric.status)
  })
})
