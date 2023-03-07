import Keycloak from 'keycloak-js'

const keycloak = ref<Keycloak>()

const useKeycloak = () => {

  const setKeycloak = (kc: Keycloak) => {
    keycloak.value = kc
  }

  return { keycloak, setKeycloak }
}

export default useKeycloak
