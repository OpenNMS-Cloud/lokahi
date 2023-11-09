const mismatchedDirNames: Record<string, string> = {
  spinner: 'progress',
  'rail-item': 'navigation-rail',
  'app-rail-nav-item': 'app-rail',
  'app-bar-link': 'app-bar',
  'app-header': 'app-rail',
  'checkbox-group': 'checkbox',
  'list-item': 'list',
  'list-header': 'list',
  'list-switch': 'list',
  'expansion-panel': 'expansion',
  'radio-group': 'radio',
  'chip': 'chips',
  'chip-list': 'chips',
  'dropdown-item': 'dropdown',
  'tab': 'tabs',
  'tab-container': 'tabs',
  'tab-panel': 'tabs'
}

const getDirectory = (partialComponentName: string): string => {
  const directoryName = partialComponentName.replace(/[A-Z]/g, (letter) => `-${letter.toLowerCase()}`).slice(1)

  // check if directory does not match component name
  if (mismatchedDirNames[directoryName]) {
    return mismatchedDirNames[directoryName]
  }

  return directoryName
}

const featherResolver = (componentName: string) => {
  if (componentName.startsWith('Feather')) {
    const partialComponentName = componentName.slice(7)
    return { name: componentName, from: `@featherds/${getDirectory(partialComponentName)}` }
  }
}

export default featherResolver
