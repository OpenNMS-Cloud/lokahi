import { DiscoverySNMPMeta, IcmpActiveDiscoveryPlusTags, NewOrUpdatedDiscovery, ServerDiscoveries } from '@/types/discovery'


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

export const activeDiscoveryFromClientToServer = (discovery: NewOrUpdatedDiscovery) => {
  const meta = discovery.meta as DiscoverySNMPMeta
  return {request:{
    id: discovery.id,
    ipAddresses: meta.ipRanges,
    locationId: discovery.locations?.[0].id,
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
        udpPorts: d?.details?.snmpConfig?.ports || []
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