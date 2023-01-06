import { mount } from '@vue/test-utils'
import CustomFeatherStep from '@/components/Common/Stepper/CustomFeatherStep.vue'
import CustomFeatherStepper from '@/components/Common/Stepper/CustomFeatherStepper.vue'

const stepWrapper1 = mount(CustomFeatherStep, {
  slots: {
    default: 'Test Content'
  },
  props: {
    nextBtnText: 'Continue'
  }
})

const stepWrapper2 = mount(CustomFeatherStep, {
  slots: {
    default: 'Test Content2'
  },
  props: {
    prevBtnText: 'Back'
  }
})

const stepperWrapper = mount(CustomFeatherStepper, {
  slots: {
    default: [
      h(document.getElementById('content') as HTMLElement, stepWrapper1.vm),
      h(document.getElementById('content') as HTMLElement, stepWrapper2.vm)
    ]
  },
  attachTo: document.body
})

test('CustomFeatherStep mount', () => {
  expect(stepWrapper1).toBeTruthy()
})

test('CustomFeatherStepper mount', () => {
  expect(stepperWrapper).toBeTruthy()
})

test('CustomFeatherStepper default slot', () => {
  expect(stepWrapper1.html()).toContain('Test Content')
})

test('CustomFeatherStepper default slot', () => {
  expect(stepWrapper2.html()).toContain('Test Content2')
})

test('Next btn text prop', async () => {
  const nextBtnText = stepperWrapper.get('[data-test="next-btn"] > span')
  expect(nextBtnText.element.innerHTML).toBe('Continue')
})

test('Prev btn text prop', async () => {
  const nextBtnText = stepperWrapper.get('[data-test="next-btn"] > span')
  await nextBtnText.trigger('click')
  const prevBtnText = stepperWrapper.get('[data-test="prev-btn"] > span')
  expect(prevBtnText.element.innerHTML).toBe('Back')
})
