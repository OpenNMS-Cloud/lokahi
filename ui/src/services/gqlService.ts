import { definePlugin, createClient, defaultPlugins } from 'villus'
import Keycloak from 'keycloak-js'
import useSnackbar from '@/composables/useSnackbar'

const { showSnackbar } = useSnackbar()

// creates a villus gql client instance
const getGqlClient = (kc: Keycloak) => {
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
    if (!error || !response) {
      return
    }

    const serverTiming = response.headers.get('server-timing')

    let traceDetails = ''

    if (serverTiming) {
      // This is added by our ingress-nginx config. We only care about the trace ID.
      // traceparent format: https://www.w3.org/TR/trace-context/#traceparent-header
      const pattern = /traceparent;desc="([0-9a-f]{2})-([0-9a-f]{32})-([0-9a-f]{16})-([0-9a-f]{2})"/
      const result = pattern.exec(serverTiming)

      if (result && result.length > 1) {
        traceDetails = ` (Trace ID: ${result[2]})`
      }
    }

    const errorsMsgs = [
      {
        err: 'INVALID_ARGUMENT:',
        msg: error.message.split('INVALID_ARGUMENT:')[1]
      },
      {
        err: 'INTERNAL:',
        msg: error.message.split('INTERNAL:')[1]
      },
      {
        err: 'ALREADY_EXISTS:',
        msg: error.message.split('ALREADY_EXISTS:')[1]
      },
      {
        err: 'UNAVAILABLE:',
        msg: `Service unavailable (${error.message.split('UNAVAILABLE:')[1]})`
      },
      {
        err: 401,
        msg: 'Using invalid or expired credentials.'
      },
      {
        err: 403,
        msg: 'The user does not have access rights.'
      },
      {
        err: 409,
        msg: 'Conflicting entry has occurred.'
      },
      {
        err: 500,
        msg: 'Server error has occurred.'
      }
    ]

    const hasError = (errorCode: number | string) => {
      const strError = errorCode.toString()

      return response?.status === errorCode || error.message.includes(strError)
    }

    const notificationMsg = () => {
      const errorObj = errorsMsgs.filter(({ err }) => hasError(err))?.[0];

      return (errorObj?.msg || 'An unknown error has occurred.') + traceDetails
    }

    showSnackbar({
      msg: notificationMsg(),
      error: true
    })

    console.log('GQL Error: ', error)
  })
})

export { getGqlClient }
