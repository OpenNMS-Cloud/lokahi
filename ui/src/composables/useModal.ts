const useModal = () => {
  const isVisible = ref(false)
  const openModal = () => (isVisible.value = true)
  const closeModal = () => (isVisible.value = false)

  onKeyStroke('Escape', () => closeModal())

  return { openModal, closeModal, isVisible }
}

export default useModal
