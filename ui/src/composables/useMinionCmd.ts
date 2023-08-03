const useMinionCmd = () => {
  const password = ref('')
  const minionId = ref('default')

  const setPassword = (pass: string) => (password.value = pass)
  const setMinionId = (minionIdString: string) => (minionId.value = minionIdString)
  const clearMinionCmdVals = () => {
    password.value = ''
    minionId.value = 'default'
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
      `-e MINION_ID='${minionId.value}'`,
      `--mount type=bind,source="/PATH_TO_DOWNLOADED_FILE/${minionId.value}-certificate.p12",target="/opt/karaf/minion.p12",readonly`,
      `opennms/lokahi-minion:latest`
    ].join(' ')
  )

  return { minionDockerCmd, setPassword, setMinionId, clearMinionCmdVals }
}

export default useMinionCmd
