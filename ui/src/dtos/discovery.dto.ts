import { REGEX_EXPRESSIONS } from '@/components/Discovery/discovery.constants'
import { validationErrorsToStringRecord } from '@/services/validationService'
import { DiscoverySNMPMeta, IcmpActiveDiscoveryPlusTags, NewOrUpdatedDiscovery, ServerDiscoveries } from '@/types/discovery'
import * as yup from 'yup'


export const activeDiscoveryFromClientToServerDTO = (discoveryInfo: IcmpActiveDiscoveryPlusTags) => {
  return {
    id:discoveryInfo.id,
    ipAddresses:discoveryInfo.ipAddresses,
    locationId:discoveryInfo.locationId,
    name:discoveryInfo.name,
    snmpConfig:discoveryInfo.snmpConfig,
    tags:discoveryInfo.tags
  }
}

export const discoveryFromClientToServer = (discovery: NewOrUpdatedDiscovery) => {
  const meta = discovery.meta as DiscoverySNMPMeta
  return {request:{
    id: discovery.id,
    ipAddresses: meta.ipRanges,
    locationId: discovery.locations?.[0]?.id,
    name: discovery.name,
    snmpConfig: {ports: meta.udpPorts?.map((b) => Number(b)), readCommunities: meta.communityStrings},
    tags: discovery.tags?.map((t) => ({name:t.name}))
  }}
}


export const discoveryFromServerToClient = (dataIn: ServerDiscoveries, locations: Array<{id: number}>) => {
  const combined: Array<NewOrUpdatedDiscovery> = []
  dataIn.listActiveDiscovery?.forEach((d) => {
    combined.push({
      id:d.details?.id,
      name: d.details?.name,
      tags: d.details?.tags,
      type: d.discoveryType,
      locations: locations.filter((b) => {return b.id === Number(d.details?.locationId)}),
      meta: {
        communityStrings: d?.details?.snmpConfig?.readCommunities || [],
        ipRanges: d?.details?.ipAddresses || [],
        udpPorts: d?.details?.snmpConfig?.ports || [],
        clientId: d?.details?.clientId,
        clientSecret: d?.details?.clientSecret,
        clientSubscriptionId: d?.details?.subscriptionId,
        directoryId: d?.details?.directoryId
      }
    })
  })
  dataIn.passiveDiscoveries?.forEach((d) => {
    combined.push({
      id:d.id,
      name: d.name
    })
  })
  return combined
}

export const clientToServerValidation = async (selectedDiscovery: NewOrUpdatedDiscovery) => {

  const discoveryValidation = yup.object().shape({
    name: yup.string().required('Please enter a name.'),
    ip: yup.string().required('Please enter an IP.').matches(new RegExp(REGEX_EXPRESSIONS.IP[0]), 'Single IP address only. You cannot enter a range.'),
    communityString: yup.string(),
    port: yup.number()
  }).required()

  let isValid = true
  let validationErrors = {}
  try {
    await discoveryValidation.validate(selectedDiscovery)
  }catch(e){
    validationErrors = validationErrorsToStringRecord(e as yup.ValidationError)
    console.log('ERRS AFTER',validationErrors)
    isValid = false
  }
  console.log('CLIEWNT TO SZXEDVD',isValid,validationErrors)
  return {isValid,validationErrors}
}