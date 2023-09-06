import axios, { AxiosRequestHeaders } from 'axios'
import useSpinner from '@/composables/useSpinner'
import keycloakConfig from '../../keycloak.config'
import useKeycloak from '@/composables/useKeycloak'

const { startSpinner, stopSpinner } = useSpinner()
const { keycloak } = useKeycloak()

const auth = axios.create({
  baseURL: `${keycloakConfig.url}/realms/${keycloakConfig.realm}/protocol/openid-connect`
})

auth.interceptors.request.use(
  (config) => {
    config.headers = {
      Authorization: `Bearer ${keycloak.value?.tokenParsed}`
    } as unknown as AxiosRequestHeaders
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

const logout = async (): Promise<void> => {
  const params = new URLSearchParams()
  params.append('client_id', keycloakConfig.clientId as string)
  params.append('id_token_hint', keycloak.value?.token as string)
  const realm = import.meta.env.VITE_KEYCLOAK_REALM || 'opennms'
  const baseUrl = import.meta.env.VITE_KEYCLOAK_URL || '/auth'
  params.append('post_logout_redirect_uri',  `${baseUrl}/realms/${realm}/protocol/openid-connect/logout`)

  startSpinner()

  try {
    await auth.post('/logout', params)
  } finally {
    stopSpinner()
    location.reload()
  }
}

export { logout }
