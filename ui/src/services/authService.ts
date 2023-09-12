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
