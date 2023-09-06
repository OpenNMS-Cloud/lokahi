import { DiscoveryType, REGEX_EXPRESSIONS } from '@/components/Discovery/discovery.constants'
import { validationErrorsToStringRecord } from '@/services/validationService'
import { DiscoveryAzureMeta, DiscoverySNMPMeta, DiscoveryTrapMeta, NewOrUpdatedDiscovery, ServerDiscoveries } from '@/types/discovery'
import * as yup from 'yup'


export const splitStringOnSemiCommaOrDot = (inString?: string) => {
  return inString?.split(/[;,]+/) || []
}

  
export const discoveryFromClientToServer = (discovery: NewOrUpdatedDiscovery) => {


  if (discovery.type === DiscoveryType.Azure) return discoveryFromAzureClientToServer(discovery)
  if (discovery.type === DiscoveryType.ICMP) return discoveryFromActiveClientToServer(discovery)
  if (discovery.type === DiscoveryType.SyslogSNMPTraps) return discoveryFromTrapClientToServer(discovery)
  return {}
}

export const discoveryFromActiveClientToServer = (discovery: NewOrUpdatedDiscovery) => {
  const meta = discovery.meta as DiscoverySNMPMeta
  return {
    id: discovery.id,
    ipAddresses: splitStringOnSemiCommaOrDot(meta.ipRanges),
    locationId: discovery.locations?.[0]?.id,
    name: discovery.name,
    snmpConfig: {ports: splitStringOnSemiCommaOrDot(meta.udpPorts), readCommunities: splitStringOnSemiCommaOrDot(meta.communityStrings)},
    tags: discovery.tags?.map((t) => ({name:t.name}))
  }
}

export const discoveryFromAzureClientToServer = (discovery: NewOrUpdatedDiscovery) => {
  const meta = discovery.meta as DiscoveryAzureMeta
  return {
    id: discovery.id,
    locationId: discovery.locations?.[0]?.id,
    name: discovery.name,
    tags: discovery.tags?.map((t) => ({name:t.name})),
    clientId: meta.clientId,
    clientSubscriptionId: meta.clientSubscriptionId,
    directoryId: meta.directoryId
  }
}

export const discoveryFromTrapClientToServer = (discovery: NewOrUpdatedDiscovery) => {
  const meta = discovery.meta as DiscoveryTrapMeta
  return {
    id: discovery.id,
    locationId: discovery.locations?.[0]?.id,
    name: discovery.name,
    snmpPorts: splitStringOnSemiCommaOrDot(meta.udpPorts), 
    toggle: meta.toggle,
    snmpCommunities: splitStringOnSemiCommaOrDot(meta.communityStrings),
    tags: discovery.tags?.map((t) => ({name:t.name}))
  }
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
        communityStrings: d?.details?.snmpConfig?.readCommunities.join(';') || '',
        ipRanges: d?.details?.ipAddresses?.join(';') || '',
        udpPorts: d?.details?.snmpConfig?.ports.join(';') || '',
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
      name: d.name,
      tags: d.tags,
      locations: locations.filter((b) => {return b.id === Number(d.locationId)}),
      type: DiscoveryType.SyslogSNMPTraps,
      meta: {
        udpPorts: d?.snmpPorts?.join(';'),
        communityStrings: d?.snmpCommunities?.join(';'),
        toggle:{toggle:d.toggle,id:d.id}
      }
    })
  })
  return combined
}
const activeDiscoveryValidation = yup.object().shape({
  name: yup.string().required('Please enter a name.'),
  locations:yup.array().of(yup.object().shape({id: yup.number(), location: yup.string().required('Location required')}).required('sdfsdf')).min(1,'Must have at least one location.'),
  ipAddresses: yup.array().of(yup.string().required('Please enter an ip address.').matches(new RegExp(REGEX_EXPRESSIONS.IP[0]), 'Single IP address only. You cannot enter a range.')),
  snmpConfig: yup.object({
    communityStrings: yup.string(),
    udpPorts: yup.number()
  }).required('required')
}).required()

const passiveDiscoveryValidation = yup.object().shape({
  name: yup.string().required('Please enter a name.'),
  locations:yup.array().of(yup.object().shape({id: yup.number(), location: yup.string().required('Location required')}).required('sdfsdf')).min(1,'Must have at least one location.'),
  snmpConfig: yup.object({
    communityStrings: yup.string(),
    udpPorts: yup.number()
  }).required('required')
}).required()

const azureDiscoveryValidation = yup.object().shape({
  name: yup.string().required('Please enter a name.'),
  ip: yup.string().required('Please enter an IP.').matches(new RegExp(REGEX_EXPRESSIONS.IP[0]), 'Single IP address only. You cannot enter a range.'),
  communityString: yup.string(),
  port: yup.number()
}).required()

const validatorMap: Record<string,yup.Schema> = {
  [DiscoveryType.Azure]: azureDiscoveryValidation,
  [DiscoveryType.ICMP]: activeDiscoveryValidation,
  [DiscoveryType.SyslogSNMPTraps]: passiveDiscoveryValidation
}
export const clientToServerValidation = async (selectedDiscovery: NewOrUpdatedDiscovery) => {

  const type = selectedDiscovery.type || ''
  const validatorToUse = validatorMap[type] || {validate: () => ({})}

  let isValid = true
  let validationErrors = {}
  const convertedDiscovery = discoveryFromClientToServer(selectedDiscovery)
  console.log('CONVERTED!',convertedDiscovery)
  try {
    await validatorToUse.validate(convertedDiscovery, { abortEarly: false })
    console.log('CLEAN!')
  }catch(e){
    validationErrors = validationErrorsToStringRecord(e as yup.ValidationError)
    console.log('VALIDATION ERRORS!',validationErrors)
    isValid = false
  }
  return {isValid,validationErrors}
}