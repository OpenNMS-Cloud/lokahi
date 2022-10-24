import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import VueKeycloak from '@dsb-norge/vue-keycloak-js'
import { KeycloakInstance } from '@dsb-norge/vue-keycloak-js/dist/types'
import keycloakConfig from '../keycloak.config'
import useKeycloak from './composables/useKeycloak'
import '@featherds/styles'
import '@featherds/styles/themes/open-light.css'
import dateFormatDirective from './directives/v-date'
import featherInputFocusDirective from './directives/v-focus'
import { getGqlClient } from './services/gqlService'

// dark / light mode
const { setKeycloak } = useKeycloak()
const isDark = useDark()

const app = createApp(App)
  .use(router)
  .use(createPinia())
  .use(VueKeycloak, {
    init: {
      onLoad: 'login-required',
      redirectUri: (() => {
        const redirectUrl = new URL(window.location.href)
        redirectUrl.searchParams.set('theme', isDark.value ? 'dark' : 'light')
        return redirectUrl.href
      })()
    },
    config: {
      realm: keycloakConfig.realm,
      url: keycloakConfig.url,
      clientId: keycloakConfig.clientId
    },
    onReady: (kc: KeycloakInstance) => {
      setKeycloak(kc)
      const gqlClient = getGqlClient(kc)
      app.use(gqlClient)
      app.mount('#app')
    }
  })
  .directive('date', dateFormatDirective)
  .directive('focus', featherInputFocusDirective)
