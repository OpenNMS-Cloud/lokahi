import { DiscoveryType, REGEX_EXPRESSIONS } from '@/components/Discovery/discovery.constants'
import { validationErrorsToStringRecord } from '@/services/validationService'
import { DiscoveryAzureMeta, DiscoverySNMPMeta, DiscoveryTrapMeta, NewOrUpdatedDiscovery, ServerDiscoveries } from '@/types/discovery'
import * as yup from 'yup'


export const splitStringOnSemiCommaOrDot = (inString?: string) => {
  return inString?.split(/[;,]+/) ?? []
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
    clientSecret: meta.clientSecret,
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
    snmpCommunities: splitStringOnSemiCommaOrDot(meta.communityStrings),
    tags: discovery.tags?.map((t) => ({name:t.name}))
  }
}
export const sortDiscoveriesByName = (a: {name?:string}, b: {name?:string}) => {
  let ret = 0
  if (!a.name) a.name = ''
  if (!b.name) b.name = ''

  if (a.name < b.name) ret = -1
  if (a.name > b.name) ret = 1
  return ret
}
export const discoveryFromServerToClient = (dataIn: ServerDiscoveries, locations: Array<{id: number}>) => {
  let combined: Array<NewOrUpdatedDiscovery> = []
  dataIn.listActiveDiscovery?.forEach((d) => {
    combined.push({
      id:d.details?.id,
      name: d.details?.name,
      tags: d.details?.tags,
      type: d.discoveryType,
      locations: locations.filter((b) => {return b.id === Number(d.details?.locationId)}),
      meta: {
        communityStrings: d?.details?.snmpConfig?.readCommunities.join(';') ?? '',
        ipRanges: d?.details?.ipAddresses?.join(';') ?? '',
        udpPorts: d?.details?.snmpConfig?.ports.join(';') ?? '',
        clientId: d?.details?.clientId,
        clientSecret: d?.details?.clientSecret ?? '',
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
  combined = combined.sort(sortDiscoveriesByName) 
  return combined
}
const activeDiscoveryValidation = yup.object().shape({
  name: yup.string().required('Please enter a name.'),
  locationId:yup.string().required('Location required.'),
  ipAddresses: yup.array().min(1,'Please enter an ip address.').of(yup.string().required('Please enter an IP address.').matches(new RegExp(REGEX_EXPRESSIONS.IP[0]), 'Single IP address only. You cannot enter a range.')),
  snmpConfig: yup.object({
    communityStrings: yup.array().of(yup.string().required('Please enter a community string.')),
    udpPorts: yup.array().of(yup.number())
  }).required('required')
}).required()

const passiveDiscoveryValidation = yup.object().shape({
  name: yup.string().required('Please enter a name.'),
  locationId:yup.string().required('Location required.'),
  snmpConfig: yup.object({
    communityStrings: yup.array().of(yup.string().required('Please enter a community string.')),
    udpPorts: yup.array().of(yup.number())
  }).required('required')
}).required()

const azureDiscoveryValidation = yup.object().shape({
  name: yup.string().required('Please enter a name.'),
  locationId:yup.string().required('Location required.'),
  clientId: yup.string().required('Client ID is required.'),
  clientSubscriptionId: yup.string().required('Client subscription ID is required.'),
  directoryId: yup.string().required('Directory ID is required.'),
  clientSecret: yup.string().required('Client secret is required.')
}).required()

const validatorMap: Record<string,yup.Schema> = {
  [DiscoveryType.Azure]: azureDiscoveryValidation,
  [DiscoveryType.ICMP]: activeDiscoveryValidation,
  [DiscoveryType.SyslogSNMPTraps]: passiveDiscoveryValidation
}
export const clientToServerValidation = async (selectedDiscovery: NewOrUpdatedDiscovery) => {

  const type = selectedDiscovery.type ?? ''
  const validatorToUse = validatorMap[type] ?? {validate: () => ({})}

  let isValid = true
  let validationErrors = {}
  const convertedDiscovery = discoveryFromClientToServer(selectedDiscovery)
  try {
    await validatorToUse.validate(convertedDiscovery, { abortEarly: false })
  }catch(e){
    validationErrors = validationErrorsToStringRecord(e as yup.ValidationError)
    isValid = false
  }
  return {isValid,validationErrors}
}