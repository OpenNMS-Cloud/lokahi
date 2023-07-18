import { QueryApi } from 'villus'

const useAtomicAutocomplete = (
  searchFn: (searchTerm: string) => Promise<void> | QueryApi<any, { searchTerm: string }>,
  getResultsLength: () => number,
  itemClickedCallback: (...p: any) => any
) => {
  const autoCompleteOpen = ref(false)
  const inputValue = ref('')

  const isAutoCompleteOpen = computed(() => {
    let open = false
    if (autoCompleteOpen.value) {
      if (inputValue.value) {
        open = true
      }
      if (getResultsLength() > 0) {
        open = true
      }
    }
    return open
  })

  const closeAutocomplete = () => {
    autoCompleteOpen.value = false
    document.removeEventListener('click', closeChecker)
  }

  const closeChecker = (e: MouseEvent) => {
    if (!(e?.target as HTMLInputElement).closest('.atomic-input-wrapper')) {
      closeAutocomplete()
    }
  }

  const wrapperClicked = () => {
    autoCompleteOpen.value = true
    searchFn(inputValue.value)
    document.addEventListener('click', closeChecker)
  }

  const onFocusLost = () => {
    closeAutocomplete()
  }

  const itemClicked = (item: unknown, index: number) => {
    if (item) {
      itemClickedCallback({ name: item as string, id: index.toString() })
    }
  }

  const textChanged = (newVal: string) => {
    if (newVal) {
      autoCompleteOpen.value = true
    }
    inputValue.value = newVal
    searchFn(newVal)
  }

  return { isAutoCompleteOpen, closeAutocomplete, wrapperClicked, itemClicked, onFocusLost, textChanged, inputValue }
}

export default useAtomicAutocomplete
