const isDark = useDark({
  selector: 'body',
  attribute: 'class',
  valueDark: 'open-dark',
  valueLight: 'open-light',
  storageKey: 'theme'
})

const toggleDark = useToggle(isDark)

const useTheme = () => {
  let onThemeChangeCallback: () => void

  const onThemeChange = (callback: () => void) => (onThemeChangeCallback = callback)

  watch(isDark, () => {
    if (onThemeChangeCallback) {
      onThemeChangeCallback()
    }
  })

  return { isDark, toggleDark, onThemeChange }
}

export default useTheme
