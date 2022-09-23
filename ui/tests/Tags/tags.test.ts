import Tags from '@/components/Tags/Tags.vue'
import { mount } from '@vue/test-utils'
import featherInputFocusDirective from '@/directives/v-focus'

const wrapper = mount(Tags, {
  global: {
    directives: {
      'focus': featherInputFocusDirective
    }
  }
})

test('The component mounts', () => {
  expect(wrapper).toBeTruthy()
})

test('The add btn should enable after a tag is entered', async () => {
  const input = wrapper.get('[data-test="add-tag-input"] .feather-input')
  const btn = wrapper.get('[data-test="add-tag-btn"]')

  expect(btn.attributes('aria-disabled')).toBe('true')
  await input.setValue('tag')
  expect(btn.attributes('aria-disabled')).toBeUndefined()
})

test('Should not allow duplicate tag names', async () => {
  const input = wrapper.get('[data-test="add-tag-input"] .feather-input')
  const btn = wrapper.get('[data-test="add-tag-btn"]')

  await input.setValue('')
  expect(btn.attributes('aria-disabled')).toBe('true')

  await input.setValue('test')
  await btn.trigger('click')
  await input.setValue('test')
  expect(btn.attributes('aria-disabled')).toBe('true')
})
