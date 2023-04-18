import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import VueKeycloak from '@dsb-norge/vue-keycloak-js'
import Keycloak from 'keycloak-js'
import keycloakConfig from '../keycloak.config'
import useKeycloak from './composables/useKeycloak'
import '@featherds/styles'
import '@featherds/styles/themes/open-light.css'
import dateFormatDirective from './directives/v-date'
import featherInputFocusDirective from './directives/v-focus'
import tabIndexDirective from './directives/v-tabindex'
import { getGqlClient } from './services/gqlService'

const { setKeycloak } = useKeycloak()

const app = createApp(App)
  .use(router)
  .use(createPinia())
  .use(VueKeycloak, {
    init: {
      onLoad: 'login-required',
      redirectUri: window.location.href
    },
    config: {
      realm: keycloakConfig.realm,
      url: keycloakConfig.url,
      clientId: keycloakConfig.clientId
    },
    onReady: (kc: Keycloak) => {
      setKeycloak(kc)
      const gqlClient = getGqlClient(kc)
      app.use(gqlClient)
      app.mount('#app')
    }
  })
  .directive('date', dateFormatDirective)
  .directive('focus', featherInputFocusDirective)
  .directive('tabindex', tabIndexDirective)
