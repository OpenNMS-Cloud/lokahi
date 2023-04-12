
const addTheme = () => {
  const theme = window.localStorage.getItem('theme')
  const isDark = theme === 'dark'
  const html = document.querySelector('html')

  const appendThemeClass = () => {
    if (isDark) {
      html.className = 'open-dark'
    } else {
      html.className = 'open-light'
    }
  }

  // init
  appendThemeClass()
}
