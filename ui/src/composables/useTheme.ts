const isDark = useDark({
  selector: 'body',
  attribute: 'class',
  valueDark: 'open-light', // forces light for EAR, may remove after FMA+
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

  return { isDark: ref(false), toggleDark, onThemeChange }
}

export default useTheme
