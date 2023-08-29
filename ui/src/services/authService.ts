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
  params.append('refresh_token', keycloak.value?.refreshToken as unknown as string)
  params.append('redirect_uri', import.meta.env.VITE_KEYCLOAK_URL + '/realms/opennms/protocol/openid-connect/logout')

  startSpinner()

  try {
    await auth.post('/logout', params)
  } finally {
    stopSpinner()
    location.reload()
  }
}

export { logout }
