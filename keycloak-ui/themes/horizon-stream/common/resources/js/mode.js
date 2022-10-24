(function checkTheme() {
  // check param on the redirectUri for dark mode
  const params = new URLSearchParams(window.location.search)
  const redirectUri = params.get('redirect_uri')

  let isDark;

  // First check redirectUri for theme
  if (redirectUri) {
    const appUrl = new URL(redirectUri)
    if (appUrl.searchParams.get('theme')) {
      isDark = appUrl.searchParams.get('theme') === 'dark'
    }
  }

  // then check localStorage
  if (isDark == null) {
    const localStorageTheme = window.localStorage.getItem('theme')
    isDark = localStorageTheme !== 'light' // default to dark if not set
  }
  
  if (isDark) {
    // remove the light theme, dark will apply
    const lightStyle = document.querySelector('link[href$="open-light.css"]')
    if (lightStyle) lightStyle.remove()
  }

  // save to localStorage
  window.localStorage.setItem('theme', isDark ? 'dark' : 'light')
}())
