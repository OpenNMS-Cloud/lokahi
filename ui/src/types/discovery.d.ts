import { IcmpActiveDiscovery } from './graphql'

interface IKey {
  [key: string]: any
}

export interface DiscoveryInput extends IKey {
  id: number | null
  type: number
  name: string
  location: string[]
  tags: string[]
  IPRange: string
  communityString?: string | null
  UDPPort?: number | string | null
}

export interface DiscoveryTrapMeta {
  communityStrings?: Array<string>;
  udpPorts?: Array<string>;
}

export interface DiscoverySNMPMeta extends DiscoveryTrapMeta{
  ipRanges?: string[];
}

export interface DiscoverySNMPV3Meta extends DiscoverySNMPMeta {
  username?: string;
  context?: string;
}

export interface DiscoverySNMPV3Auth extends DiscoverySNMPV3Meta {
  password?: string;
  usePasswordAsKey?: boolean;
}

export interface DiscoverySNMPV3AuthPrivacy extends DiscoverySNMPV3Auth {
  privacy?: string;
  usePrivacyAsKey?: boolean;
}



export interface DiscoveryAzureMeta {
  clientId?: string;
  clientSecret?: string;
  clientSubscriptionId?: string;
  directoryId?: string;
}

export type DiscoveryMeta = DiscoverySNMPMeta | DiscoveryTrapMeta | DiscoveryAzureMeta;

export interface NewOrUpdatedDiscovery {
  id?: number;
  name?: string;
  tags?: Array<Tag>;
  locations?: Array<any>;
  type?: string;
  meta?: DiscoveryMeta;
}


export interface DiscoveryStore {
  discoveryFormActive: boolean;
  foundLocations: Array<any>;
  foundTags: Array<string>;
  loadedDiscoveries: Array<NewOrUpdatedDiscovery>;
  loading: boolean;
  locationSearch: string;
  locationError: string;
  selectedDiscovery: NewOrUpdatedDiscovery;
  soloTypeEditor: boolean;
  soloTypePageActive: boolean;
  tagSearch: string;
  tagError: string;
}

export interface ServerDiscovery {
  discoveryType?: string;
  details?: {id?: number, tags?:Array<Tag>, ipAddresses?: Array<string>,locations?:Array<Location>,locationId:string,name:string,snmpConfig?: {ports: Array<string>,readCommunities:Array<string>}}
}
export interface PassiveServerDiscovery {

  id?: any; 
  locationId?: string | undefined;
   name?: string | undefined; 
   snmpCommunities?: string[] | undefined; 
   snmpPorts?: number[] | undefined; toggle: boolean; 
}
export interface ServerDiscoveries {
  listActiveDiscovery?:Array<ServerDiscovery>;
  passiveDiscoveries?:Array<PassiveServerDiscovery>;
}


export interface IcmpActiveDiscoveryPlusTags extends IcmpActiveDiscovery {
  tags: Array<Tag>
}