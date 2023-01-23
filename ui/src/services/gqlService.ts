import { definePlugin, createClient, defaultPlugins } from 'villus'
import { KeycloakInstance } from '@dsb-norge/vue-keycloak-js/dist/types'
import useSnackbar from '@/composables/useSnackbar'

const { showSnackbar } = useSnackbar()

// creates a villus gql client instance
const getGqlClient = (kc: KeycloakInstance) => {
  // auth plugin to append token headers
  const authPlugin = ({ opContext }: any) => {
    opContext.headers.Authorization = `Bearer ${kc.token}`
  }

  return createClient({
    url: `${import.meta.env.VITE_BASE_URL}/graphql`,
    use: [authPlugin, errorNotificationPlugin, ...defaultPlugins()],
    cachePolicy: 'cache-and-network'
  })
}

// error notification plugin for specific status codes
const errorNotificationPlugin = definePlugin(({ afterQuery }) => {
  afterQuery(({ error }, { response }) => {
    if (!error) return

    let notificationMsg = 'An unknown error has occured.'

    const hasError = (errorCode: number) => {
      const strError = errorCode.toString()
      return response?.status === errorCode || error.message.includes(strError)
    }

    if (hasError(401)) {
      notificationMsg = 'Using invalid or expired credentials.'
    } else if (hasError(403)) {
      notificationMsg = 'The user does not have access rights.'
    } else if (hasError(409)) {
      notificationMsg = 'Conflicting entry has occured.'
    } else if (hasError(500)) {
      notificationMsg = 'Server error has occured.'
    }

    else if ('INTERNAL:') {
      notificationMsg = error.message.split('INTERNAL:')[1]
    }

    showSnackbar({
      msg: notificationMsg,
      error: true
    })

    console.log('GQL Error: ', error)
  })
})

export { getGqlClient }
