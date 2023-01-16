const useModal = () => {
  const isVisible = ref(false)
  const openModal = () => (isVisible.value = true)
  const closeModal = () => (isVisible.value = false)

  // lets us close modal with esc key
  onKeyStroke('Escape', () => closeModal())

  return { openModal, closeModal, isVisible }
}

export default useModal
