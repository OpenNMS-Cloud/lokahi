import AppliancesAddNodeCtrl from '@/components/Appliances/AppliancesAddNodeCtrl.vue'
import { createTestingPinia } from '@pinia/testing'
import { mount } from '@vue/test-utils'
import { createClient, VILLUS_CLIENT } from 'villus'
import { useNodeMutations } from '@/store/Mutations/nodeMutations'

const wrapper = mount(AppliancesAddNodeCtrl, {
  global: {
    stubs: {
      teleport: true
    },
    plugins: [createTestingPinia()],
    provide: {
      [VILLUS_CLIENT as unknown as string]: createClient({
        url: 'http://test/graphql'
      })
    }
  }
})

test('The component mounts', () => {
  expect(wrapper).toBeTruthy()
})

test('The modal should open when the add node btn is clicked', async () => {
  const btn = wrapper.get('[data-test="add-node-btn"]')
  const modalInput1 = wrapper.find('[data-test="name-input"]')

  // modal should be closed
  expect(modalInput1.exists()).toBeFalsy()

  await btn.trigger('click')

  // modal should be open
  const modalInput2 = wrapper.find('[data-test="name-input"]')
  expect(modalInput2.exists()).toBeTruthy()
})

test('The cancel btn should close the modal', async () => {
  await wrapper.get('[data-test="add-node-btn"]').trigger('click')
  await wrapper.get('[data-test="cancel-btn"]').trigger('click')

  const modalInput = wrapper.find('[data-test="name-input"]')
  expect(modalInput.exists()).toBeFalsy()
})

test('The save btn should enable if name is entered', async () => {
  await wrapper.get('[data-test="add-node-btn"]').trigger('click')

  const nameInput = wrapper.get('[data-test="name-input"] .feather-input')
  const saveBtn = wrapper.get('[data-test="save-btn"]')

  // should be disabled
  expect(saveBtn.attributes('aria-disabled')).toBe('true')

  await nameInput.setValue('some name')

  // should be enabled
  expect(saveBtn.attributes('aria-disabled')).toBeUndefined()
})

test('The add node mutation is called', async () => {
  const nodeMutations = useNodeMutations()
  const addNode = vi.spyOn(nodeMutations, 'addNode')

  await wrapper.get('[data-test="add-node-btn"]').trigger('click')
  await wrapper.get('[data-test="name-input"] .feather-input').setValue('some name')
  await wrapper.get('[data-test="save-btn"]').trigger('click')

  // expect save node query to be called
  expect(addNode).toHaveBeenCalledTimes(1)
})
