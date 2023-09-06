const useMinionCmd = () => {
  const password = ref('')
  const locationName = ref('default')

  const setPassword = (pass: string) => (password.value = pass)
  const setLocationName = (locationNameString: string) => (locationName.value = locationNameString)
  const clearMinionCmdVals = () => {
    password.value = ''
    locationName.value = 'default'
  }

  const url = computed<string>(() => {
    if (location.origin.includes('dev')) {
      return 'minion.onms-fb-dev.dev.nonprod.dataservice.opennms.com'
    }

    if (location.origin.includes('staging')) {
      return 'minion.onms-fb-stg.staging.nonprod.dataservice.opennms.com'
    }

    return 'minion.onms-fb-prod.production.prod.dataservice.opennms.com'
  })

  const minionDockerCmd = computed<string>(() =>
    [
      `docker run --rm`,
      `-p 162:1162/udp`,
      `-p 9999:9999/udp`,
      `-e MINION_GATEWAY_HOST="${url.value}"`,
      `-e GRPC_CLIENT_KEYSTORE_PASSWORD='${password.value}'`,
      `--mount type=bind,source="/PATH_TO_DOWNLOADED_FILE/${locationName.value}-certificate.p12",target="/opt/karaf/minion.p12",readonly`,
      `opennms/lokahi-minion:latest`
    ].join(' ')
  )

  return { minionDockerCmd, setPassword, setLocationName, clearMinionCmdVals }
}

export default useMinionCmd
