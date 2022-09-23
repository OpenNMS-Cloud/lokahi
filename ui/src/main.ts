import { createApp, h } from 'vue'
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
const dark = useDark()

const app = createApp({
  render: () => h(App)
})
  .use(router)
  .use(createPinia())
  .use(VueKeycloak, {
    init: {
      onLoad: 'login-required',
      redirectUri: `${window.location.href}${dark.value ? '?theme=dark' : '?theme=light'}`
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
