import DeleteConfirmationModal from '@/components/Common/DeleteConfirmationModal.vue'
import mount from '../mountWithPiniaVillus'
import useModal from '@/composables/useModal'

const { isVisible, closeModal, openModal } = useModal()
const name = 'Test item'
const deleteHandler = vi.fn(() => closeModal())
const isDeleting = false

const wrapper = mount({
  component: DeleteConfirmationModal,
  props: {
    isVisible,
    name,
    closeModal,
    isDeleting,
    deleteHandler
  },
  shallow: false,
  global: {
    stubs: {
      teleport: true
    }
  }
})

test('The modal mounts', () => {
  expect(wrapper).toBeTruthy()
})

test('The modal actions', async () => {
  openModal()
  expect(isVisible.value).toBe(true)

  const cancelBtn = wrapper.get('[data-test="cancel-btn"]')
  await cancelBtn.trigger('click')
  expect(isVisible.value).toBe(false)

  openModal()
  const deleteBtn = wrapper.get('[data-test="delete-btn"]')
  await deleteBtn.trigger('click')
  expect(deleteHandler).toHaveBeenCalledOnce()
  expect(isVisible.value).toBe(false)
})
