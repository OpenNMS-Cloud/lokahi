import BasicToggle from '@/components/Common/basicToggle.vue'
import { mount } from '@vue/test-utils'

const toggle = ref(false)
const onToggle = () => toggle.value = true

const wrapper = mount(BasicToggle, {
  props: {
    toggle,
    onToggle
  }
})

test('The basic toggle mounts', () => {
  expect(wrapper).toBeTruthy()
})

test('The value updates when the emit event is called', async () => {
  wrapper.vm.$emit('toggle', true)
  await wrapper.vm.$nextTick()

  expect(wrapper.emitted('toggle')).toBeTruthy()
  expect(toggle.value).toBeTruthy()
})
