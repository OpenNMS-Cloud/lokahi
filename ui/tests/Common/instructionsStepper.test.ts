import { mount } from '@vue/test-utils'
import InstructionsStepper from '@/components/Common/InstructionsStepper.vue'

const mock = {
  textButton: 'This is a text toggle button',
  stepLists: [
    {
      title: 'step1',
      items: ['instructions of step1a', 'instructions of step1b', 'instructions of step1c']
    },
    {
      title: 'step2',
      items: ['instructions of step2a', 'instructions of step2b']
    }
  ]
}

let wrapper: any

describe('InstructionsStepper', () => {
  beforeEach(() => {
    wrapper = mount(InstructionsStepper, {
      props: {
        textButton: mock.textButton,
        stepLists: mock.stepLists
      }
    })
  })

  afterAll(() => {
    wrapper.unmount()
  })

  test('Mount', () => {
    expect(wrapper).toBeTruthy()
  })

  test('Should have a toggle text button', () => {
    const btn = wrapper.get('[data-test="btn-open-toggle"]')
    expect(btn.exists()).toBeTruthy()
  })

  test('should have isntructions stepper when expand', async () => {
    await wrapper.get('[data-test="btn-open-toggle"]').trigger('click')

    const stepper = wrapper.find('[data-test="stepper"]')
    expect(stepper.exists()).toBeTruthy()
  })

  test('should display instructions of the selected step', async () => {
    await wrapper.get('[data-test="btn-open-toggle"]').trigger('click')
    await wrapper.findAll('[data-test="step-selector"]')[1].trigger('click')

    const subtitle = wrapper.find('.step-content.active > [data-test="step-content-subtitle"]')
    expect(subtitle.text()).toEqual(mock.stepLists[1].title)

    const stepContentList = wrapper.findAll('.step-content.active > [data-test="step-content-list"] > li')
    expect(stepContentList.length).toEqual(mock.stepLists[1].items.length)
  })
})
