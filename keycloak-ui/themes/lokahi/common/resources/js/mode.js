(function checkTheme() {
  // get theme from localStorage
  // const localStorageTheme = window.localStorage.getItem('theme')

  // only light mode for EAR. May remove for FMA+
  const localStorageTheme = 'light' 

  // default to dark if not set
  let isDark = localStorageTheme !== 'light'
  
  if (isDark) {
    // remove the light theme, dark will apply
    const lightStyle = document.querySelector('link[href$="open-light.css"]')
    if (lightStyle) lightStyle.remove()
  }

  // save to localStorage
  window.localStorage.setItem('theme', isDark ? 'dark' : 'light')
}())
