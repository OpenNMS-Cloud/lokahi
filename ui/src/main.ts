import { createApp, h } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import { createPinia } from 'pinia'
import VueKeycloak from '@dsb-norge/vue-keycloak-js'
import { KeycloakInstance } from '@dsb-norge/vue-keycloak-js/dist/types'
import keycloakConfig from '../keycloak.config'
import useKeycloak from './composables/useKeycloak'
import '@featherds/styles'
import '@featherds/styles/themes/open-light.css'
import dateFormatDirective from './directives/v-date'

const { setKeycloak } = useKeycloak()
const dark = useDark()

const app = createApp({
  render: () => h(App)
})
  .use(router)
  .use(createPinia())
  .use(store)
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
      app.mount('#app')
    }
  })
  .directive('date', dateFormatDirective)
