import { TypedDocumentNode as DocumentNode } from '@graphql-typed-document-node/core';
export type Maybe<T> = T;
export type InputMaybe<T> = T;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> };
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> };
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: string;
  String: string;
  Boolean: boolean;
  Int: number;
  Float: number;
  Date: any;
  Long: any;
  Map_String_IntegerScalar: any;
  Map_String_StringScalar: any;
  UNREPRESENTABLE: any;
};

export type AlarmAckDtoInput = {
  ticketId?: InputMaybe<Scalars['String']>;
  ticketState?: InputMaybe<Scalars['String']>;
  user?: InputMaybe<Scalars['String']>;
};

export type AlarmCollectionDto = {
  __typename?: 'AlarmCollectionDTO';
  alarms?: Maybe<Array<Maybe<AlarmDto>>>;
};

export type AlarmDto = {
  __typename?: 'AlarmDTO';
  ackTime?: Maybe<Scalars['Date']>;
  ackUser?: Maybe<Scalars['String']>;
  affectedNodeCount?: Maybe<Scalars['Int']>;
  applicationDN?: Maybe<Scalars['String']>;
  clearKey?: Maybe<Scalars['String']>;
  count?: Maybe<Scalars['Int']>;
  description?: Maybe<Scalars['String']>;
  firstAutomationTime?: Maybe<Scalars['Date']>;
  firstEventTime?: Maybe<Scalars['Date']>;
  id?: Maybe<Scalars['Int']>;
  ifIndex?: Maybe<Scalars['Int']>;
  ipAddress?: Maybe<Scalars['String']>;
  lastAutomationTime?: Maybe<Scalars['Date']>;
  lastEvent?: Maybe<EventDto>;
  lastEventTime?: Maybe<Scalars['Date']>;
  location?: Maybe<Scalars['String']>;
  logMessage?: Maybe<Scalars['String']>;
  managedObjectInstance?: Maybe<Scalars['String']>;
  managedObjectType?: Maybe<Scalars['String']>;
  mouseOverText?: Maybe<Scalars['String']>;
  nodeId?: Maybe<Scalars['Int']>;
  nodeLabel?: Maybe<Scalars['String']>;
  operatorInstructions?: Maybe<Scalars['String']>;
  ossPrimaryKey?: Maybe<Scalars['String']>;
  parameters?: Maybe<Array<Maybe<EventParameterDto>>>;
  qosAlarmState?: Maybe<Scalars['String']>;
  reductionKey?: Maybe<Scalars['String']>;
  reductionKeyMemo?: Maybe<ReductionKeyMemoDto>;
  relatedAlarms?: Maybe<Array<Maybe<AlarmSummaryDto>>>;
  serviceType?: Maybe<ServiceTypeDto>;
  severity?: Maybe<Scalars['String']>;
  stickyMemo?: Maybe<MemoDto>;
  suppressedBy?: Maybe<Scalars['String']>;
  suppressedTime?: Maybe<Scalars['Date']>;
  suppressedUntil?: Maybe<Scalars['Date']>;
  troubleTicket?: Maybe<Scalars['String']>;
  troubleTicketLink?: Maybe<Scalars['String']>;
  troubleTicketState?: Maybe<Scalars['Int']>;
  type?: Maybe<Scalars['Int']>;
  uei?: Maybe<Scalars['String']>;
  x733AlarmType?: Maybe<Scalars['String']>;
  x733ProbableCause?: Maybe<Scalars['Int']>;
};

export type AlarmSummaryDto = {
  __typename?: 'AlarmSummaryDTO';
  description?: Maybe<Scalars['String']>;
  id?: Maybe<Scalars['Int']>;
  label?: Maybe<Scalars['String']>;
  logMessage?: Maybe<Scalars['String']>;
  nodeLabel?: Maybe<Scalars['String']>;
  reductionKey?: Maybe<Scalars['String']>;
  severity?: Maybe<Scalars['String']>;
  type?: Maybe<Scalars['Int']>;
  uei?: Maybe<Scalars['String']>;
};

export type DeviceCollectionDto = {
  __typename?: 'DeviceCollectionDTO';
  devices?: Maybe<Array<Maybe<DeviceDto>>>;
};

export type DeviceCreateDtoInput = {
  label?: InputMaybe<Scalars['String']>;
  latitude?: InputMaybe<Scalars['Float']>;
  location?: InputMaybe<Scalars['String']>;
  longitude?: InputMaybe<Scalars['Float']>;
  managementIp?: InputMaybe<Scalars['String']>;
  monitoringArea?: InputMaybe<Scalars['String']>;
  port?: InputMaybe<Scalars['Int']>;
  snmpCommunityString?: InputMaybe<Scalars['String']>;
};

export type DeviceDto = {
  __typename?: 'DeviceDTO';
  createTime?: Maybe<Scalars['Date']>;
  domainName?: Maybe<Scalars['String']>;
  foreignId?: Maybe<Scalars['String']>;
  foreignSource?: Maybe<Scalars['String']>;
  id?: Maybe<Scalars['Int']>;
  label?: Maybe<Scalars['String']>;
  labelSource?: Maybe<Scalars['String']>;
  lastEgressFlow?: Maybe<Scalars['Date']>;
  lastIngressFlow?: Maybe<Scalars['Date']>;
  lastPoll?: Maybe<Scalars['Date']>;
  location?: Maybe<LocationDto>;
  managementIp?: Maybe<Scalars['String']>;
  netBiosName?: Maybe<Scalars['String']>;
  operatingSystem?: Maybe<Scalars['String']>;
  parentId?: Maybe<Scalars['Int']>;
  snmpCommunityString?: Maybe<Scalars['String']>;
  sysContact?: Maybe<Scalars['String']>;
  sysDescription?: Maybe<Scalars['String']>;
  sysLocation?: Maybe<Scalars['String']>;
  sysName?: Maybe<Scalars['String']>;
  sysOid?: Maybe<Scalars['String']>;
  type?: Maybe<Scalars['String']>;
};

export type Event = {
  __typename?: 'Event';
  eventInfo?: Maybe<EventInfo>;
  eventParams?: Maybe<Array<Maybe<EventParameter>>>;
  id: Scalars['Int'];
  ipAddress?: Maybe<Scalars['String']>;
  nodeId: Scalars['Int'];
  producedTime: Scalars['Long'];
  tenantId?: Maybe<Scalars['String']>;
  uei?: Maybe<Scalars['String']>;
};

export type EventCollectionDto = {
  __typename?: 'EventCollectionDTO';
  events?: Maybe<Array<Maybe<EventDto>>>;
};

export type EventDto = {
  __typename?: 'EventDTO';
  ackTime?: Maybe<Scalars['Date']>;
  ackUser?: Maybe<Scalars['String']>;
  autoAction?: Maybe<Scalars['String']>;
  correlation?: Maybe<Scalars['String']>;
  createTime?: Maybe<Scalars['Date']>;
  description?: Maybe<Scalars['String']>;
  display?: Maybe<Scalars['String']>;
  host?: Maybe<Scalars['String']>;
  id?: Maybe<Scalars['Int']>;
  ifIndex?: Maybe<Scalars['Int']>;
  ipAddress?: Maybe<Scalars['String']>;
  label?: Maybe<Scalars['String']>;
  location?: Maybe<Scalars['String']>;
  log?: Maybe<Scalars['String']>;
  logGroup?: Maybe<Scalars['String']>;
  logMessage?: Maybe<Scalars['String']>;
  mouseOverText?: Maybe<Scalars['String']>;
  nodeId?: Maybe<Scalars['Int']>;
  nodeLabel?: Maybe<Scalars['String']>;
  notification?: Maybe<Scalars['String']>;
  operationActionMenuText?: Maybe<Scalars['String']>;
  operatorAction?: Maybe<Scalars['String']>;
  operatorInstructions?: Maybe<Scalars['String']>;
  parameters?: Maybe<Array<Maybe<EventParameterDto>>>;
  pathOutage?: Maybe<Scalars['String']>;
  serviceType?: Maybe<ServiceTypeDto>;
  severity?: Maybe<Scalars['String']>;
  snmp?: Maybe<Scalars['String']>;
  snmpHost?: Maybe<Scalars['String']>;
  source?: Maybe<Scalars['String']>;
  suppressedCount?: Maybe<Scalars['Int']>;
  time?: Maybe<Scalars['Date']>;
  troubleTicket?: Maybe<Scalars['String']>;
  troubleTicketState?: Maybe<Scalars['Int']>;
  uei?: Maybe<Scalars['String']>;
};

export type EventDtoInput = {
  ackTime?: InputMaybe<Scalars['Date']>;
  ackUser?: InputMaybe<Scalars['String']>;
  autoAction?: InputMaybe<Scalars['String']>;
  correlation?: InputMaybe<Scalars['String']>;
  createTime?: InputMaybe<Scalars['Date']>;
  description?: InputMaybe<Scalars['String']>;
  display?: InputMaybe<Scalars['String']>;
  host?: InputMaybe<Scalars['String']>;
  id?: InputMaybe<Scalars['Int']>;
  ifIndex?: InputMaybe<Scalars['Int']>;
  ipAddress?: InputMaybe<Scalars['String']>;
  label?: InputMaybe<Scalars['String']>;
  location?: InputMaybe<Scalars['String']>;
  log?: InputMaybe<Scalars['String']>;
  logGroup?: InputMaybe<Scalars['String']>;
  logMessage?: InputMaybe<Scalars['String']>;
  mouseOverText?: InputMaybe<Scalars['String']>;
  nodeId?: InputMaybe<Scalars['Int']>;
  nodeLabel?: InputMaybe<Scalars['String']>;
  notification?: InputMaybe<Scalars['String']>;
  operationActionMenuText?: InputMaybe<Scalars['String']>;
  operatorAction?: InputMaybe<Scalars['String']>;
  operatorInstructions?: InputMaybe<Scalars['String']>;
  parameters?: InputMaybe<Array<InputMaybe<EventParameterDtoInput>>>;
  pathOutage?: InputMaybe<Scalars['String']>;
  serviceType?: InputMaybe<ServiceTypeDtoInput>;
  severity?: InputMaybe<Scalars['String']>;
  snmp?: InputMaybe<Scalars['String']>;
  snmpHost?: InputMaybe<Scalars['String']>;
  source?: InputMaybe<Scalars['String']>;
  suppressedCount?: InputMaybe<Scalars['Int']>;
  time?: InputMaybe<Scalars['Date']>;
  troubleTicket?: InputMaybe<Scalars['String']>;
  troubleTicketState?: InputMaybe<Scalars['Int']>;
  uei?: InputMaybe<Scalars['String']>;
};

export type EventInfo = {
  __typename?: 'EventInfo';
  snmp?: Maybe<SnmpInfo>;
};

export type EventParameter = {
  __typename?: 'EventParameter';
  encoding?: Maybe<Scalars['String']>;
  name?: Maybe<Scalars['String']>;
  type?: Maybe<Scalars['String']>;
  value?: Maybe<Scalars['String']>;
};

export type EventParameterDto = {
  __typename?: 'EventParameterDTO';
  name?: Maybe<Scalars['String']>;
  type?: Maybe<Scalars['String']>;
  value?: Maybe<Scalars['String']>;
};

export type EventParameterDtoInput = {
  name?: InputMaybe<Scalars['String']>;
  type?: InputMaybe<Scalars['String']>;
  value?: InputMaybe<Scalars['String']>;
};

export type IpInterface = {
  __typename?: 'IpInterface';
  id: Scalars['Long'];
  ipAddress?: Maybe<Scalars['String']>;
  nodeId: Scalars['Long'];
  tenantId?: Maybe<Scalars['String']>;
};

export type Location = {
  __typename?: 'Location';
  id: Scalars['Long'];
  location?: Maybe<Scalars['String']>;
  tenantId?: Maybe<Scalars['String']>;
};

export type LocationCollectionDto = {
  __typename?: 'LocationCollectionDTO';
  locations?: Maybe<Array<Maybe<LocationDto>>>;
};

export type LocationDto = {
  __typename?: 'LocationDTO';
  latitude?: Maybe<Scalars['Float']>;
  locationName?: Maybe<Scalars['String']>;
  longitude?: Maybe<Scalars['Float']>;
};

export type MemoDto = {
  __typename?: 'MemoDTO';
  author?: Maybe<Scalars['String']>;
  body?: Maybe<Scalars['String']>;
  created?: Maybe<Scalars['Date']>;
  id?: Maybe<Scalars['Int']>;
  updated?: Maybe<Scalars['Date']>;
};

export type Minion = {
  __typename?: 'Minion';
  id: Scalars['Long'];
  label?: Maybe<Scalars['String']>;
  lastCheckedTime: Scalars['Long'];
  location?: Maybe<Location>;
  locationId: Scalars['Long'];
  systemId?: Maybe<Scalars['String']>;
  tenantId?: Maybe<Scalars['String']>;
};

export type MinionCollectionDto = {
  __typename?: 'MinionCollectionDTO';
  minions?: Maybe<Array<Maybe<MinionDto>>>;
};

export type MinionDto = {
  __typename?: 'MinionDTO';
  id?: Maybe<Scalars['String']>;
  label?: Maybe<Scalars['String']>;
  lastUpdated?: Maybe<Scalars['Date']>;
  location?: Maybe<Scalars['String']>;
  status?: Maybe<Scalars['String']>;
};

/** Mutation root */
export type Mutation = {
  __typename?: 'Mutation';
  addDevice?: Maybe<Scalars['Int']>;
  addNode?: Maybe<Node>;
  clearAlarm?: Maybe<Scalars['String']>;
  createEvent?: Maybe<Scalars['Boolean']>;
  savePagerDutyConfig?: Maybe<Scalars['Boolean']>;
  toggleUsageStatsReport?: Maybe<Scalars['Boolean']>;
};


/** Mutation root */
export type MutationAddDeviceArgs = {
  device?: InputMaybe<DeviceCreateDtoInput>;
};


/** Mutation root */
export type MutationAddNodeArgs = {
  node?: InputMaybe<NodeCreateInput>;
};


/** Mutation root */
export type MutationClearAlarmArgs = {
  ackDTO?: InputMaybe<AlarmAckDtoInput>;
  id?: InputMaybe<Scalars['Long']>;
};


/** Mutation root */
export type MutationCreateEventArgs = {
  event?: InputMaybe<EventDtoInput>;
};


/** Mutation root */
export type MutationSavePagerDutyConfigArgs = {
  config?: InputMaybe<PagerDutyConfigDtoInput>;
};


/** Mutation root */
export type MutationToggleUsageStatsReportArgs = {
  toggleDataChoices?: InputMaybe<ToggleDataChoicesDtoInput>;
};

export type Node = {
  __typename?: 'Node';
  createTime: Scalars['Long'];
  id: Scalars['Long'];
  ipInterfaces?: Maybe<Array<Maybe<IpInterface>>>;
  location?: Maybe<Location>;
  monitoringLocationId: Scalars['Long'];
  nodeLabel?: Maybe<Scalars['String']>;
  tenantId?: Maybe<Scalars['String']>;
};

export type NodeCreateInput = {
  label?: InputMaybe<Scalars['String']>;
  location?: InputMaybe<Scalars['String']>;
  managementIp?: InputMaybe<Scalars['String']>;
};

export type PagerDutyConfigDtoInput = {
  integrationkey?: InputMaybe<Scalars['String']>;
};

/** Query root */
export type Query = {
  __typename?: 'Query';
  deviceById?: Maybe<DeviceDto>;
  findAllEvents?: Maybe<Array<Maybe<Event>>>;
  findAllLocations?: Maybe<Array<Maybe<Location>>>;
  findAllMinions?: Maybe<Array<Maybe<Minion>>>;
  findAllNodes?: Maybe<Array<Maybe<Node>>>;
  findEventsByNodeId?: Maybe<Array<Maybe<Event>>>;
  findLocationById?: Maybe<Location>;
  findMinionById?: Maybe<Minion>;
  findNodeById?: Maybe<Node>;
  listAlarms?: Maybe<AlarmCollectionDto>;
  listDevices?: Maybe<DeviceCollectionDto>;
  listEvents?: Maybe<EventCollectionDto>;
  listLocations?: Maybe<LocationCollectionDto>;
  listMinions?: Maybe<MinionCollectionDto>;
  locationById?: Maybe<LocationDto>;
  metric?: Maybe<TimeSeriesQueryResult>;
  minionById?: Maybe<MinionDto>;
  usageStatsReport?: Maybe<UsageStatisticsReportDto>;
};


/** Query root */
export type QueryDeviceByIdArgs = {
  id?: InputMaybe<Scalars['Int']>;
};


/** Query root */
export type QueryFindEventsByNodeIdArgs = {
  id?: InputMaybe<Scalars['Long']>;
};


/** Query root */
export type QueryFindLocationByIdArgs = {
  id: Scalars['Long'];
};


/** Query root */
export type QueryFindMinionByIdArgs = {
  id?: InputMaybe<Scalars['String']>;
};


/** Query root */
export type QueryFindNodeByIdArgs = {
  id?: InputMaybe<Scalars['Long']>;
};


/** Query root */
export type QueryLocationByIdArgs = {
  id?: InputMaybe<Scalars['String']>;
};


/** Query root */
export type QueryMetricArgs = {
  labels?: InputMaybe<Scalars['Map_String_StringScalar']>;
  name?: InputMaybe<Scalars['String']>;
  timeRange?: InputMaybe<Scalars['Int']>;
  timeRangeUnit?: InputMaybe<TimeRangeUnit>;
};


/** Query root */
export type QueryMinionByIdArgs = {
  id?: InputMaybe<Scalars['String']>;
};

export type ReductionKeyMemoDto = {
  __typename?: 'ReductionKeyMemoDTO';
  reductionKey?: Maybe<Scalars['String']>;
};

export type ServiceTypeDto = {
  __typename?: 'ServiceTypeDTO';
  id?: Maybe<Scalars['Int']>;
  name?: Maybe<Scalars['String']>;
};

export type ServiceTypeDtoInput = {
  id?: InputMaybe<Scalars['Int']>;
  name?: InputMaybe<Scalars['String']>;
};

export type SnmpInfo = {
  __typename?: 'SnmpInfo';
  community?: Maybe<Scalars['String']>;
  generic: Scalars['Int'];
  id?: Maybe<Scalars['String']>;
  specific: Scalars['Int'];
  trapOid?: Maybe<Scalars['String']>;
  version?: Maybe<Scalars['String']>;
};

export type TsData = {
  __typename?: 'TSData';
  result?: Maybe<Array<Maybe<TsResult>>>;
  resultType?: Maybe<Scalars['String']>;
};

export type TsResult = {
  __typename?: 'TSResult';
  metric?: Maybe<Scalars['Map_String_StringScalar']>;
  value?: Maybe<Array<Maybe<Scalars['Float']>>>;
  values?: Maybe<Array<Maybe<Array<Maybe<Scalars['Float']>>>>>;
};

export enum TimeRangeUnit {
  Day = 'DAY',
  Hour = 'HOUR',
  Minute = 'MINUTE',
  Second = 'SECOND',
  Week = 'WEEK'
}

export type TimeSeriesQueryResult = {
  __typename?: 'TimeSeriesQueryResult';
  data?: Maybe<TsData>;
  status?: Maybe<Scalars['String']>;
};

export type ToggleDataChoicesDtoInput = {
  toggle: Scalars['Boolean'];
};

export type UsageStatisticsReportDto = {
  __typename?: 'UsageStatisticsReportDTO';
  deviceTypeCounts?: Maybe<Scalars['Map_String_IntegerScalar']>;
  monitoredServices: Scalars['Long'];
  nodes: Scalars['Long'];
  systemId?: Maybe<Scalars['String']>;
  version?: Maybe<Scalars['String']>;
};

export type AlarmsQueryVariables = Exact<{ [key: string]: never; }>;


export type AlarmsQuery = { __typename?: 'Query', listAlarms?: { __typename?: 'AlarmCollectionDTO', alarms?: Array<{ __typename?: 'AlarmDTO', id?: number, description?: string, severity?: string, lastEventTime?: any }> } };

export type ClearAlarmMutationVariables = Exact<{
  id: Scalars['Long'];
  ackDTO: AlarmAckDtoInput;
}>;


export type ClearAlarmMutation = { __typename?: 'Mutation', clearAlarm?: string };

export type CreateEventMutationVariables = Exact<{
  event: EventDtoInput;
}>;


export type CreateEventMutation = { __typename?: 'Mutation', createEvent?: boolean };

export type ChartPartsFragment = { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, values?: Array<Array<number>> }> } };

export type ChartTimeSeriesMetricFragment = { __typename?: 'Query', metric?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, values?: Array<Array<number>> }> } } };

export type DeviceUptimePartsFragment = { __typename?: 'Query', deviceUptime?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } } };

export type DeviceLatencyPartsFragment = { __typename?: 'Query', deviceLatency?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } } };

export type MetricPartsFragment = { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } };

export type TimeSeriesMetricFragment = { __typename?: 'Query', metric?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } } };

export type MinionUptimePartsFragment = { __typename?: 'Query', minionUptime?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } } };

export type MinionLatencyPartsFragment = { __typename?: 'Query', minionLatency?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } } };

export type NodesUptimePartsFragment = { __typename?: 'Query', nodesUptime?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } } };

export type NodesLatencyPartsFragment = { __typename?: 'Query', nodesLatency?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } } };

export type NodeLatencyPartsFragment = { __typename?: 'Query', nodeLatency?: { __typename?: 'TimeSeriesQueryResult', status?: string, data?: { __typename?: 'TSData', resultType?: string, result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number>, values?: Array<Array<number>> }> } } };

export type AddNodeMutationVariables = Exact<{
  node: NodeCreateInput;
}>;


export type AddNodeMutation = { __typename?: 'Mutation', addNode?: { __typename?: 'Node', createTime: any, id: any, monitoringLocationId: any, nodeLabel?: string, tenantId?: string } };

export type NodesPartsFragment = { __typename?: 'Query', findAllNodes?: Array<{ __typename?: 'Node', createTime: any, id: any, monitoringLocationId: any, nodeLabel?: string, tenantId?: string, ipInterfaces?: Array<{ __typename?: 'IpInterface', id: any, ipAddress?: string, nodeId: any, tenantId?: string }>, location?: { __typename?: 'Location', id: any, location?: string, tenantId?: string } }> };

export type SavePagerDutyConfigMutationVariables = Exact<{
  config: PagerDutyConfigDtoInput;
}>;


export type SavePagerDutyConfigMutation = { __typename?: 'Mutation', savePagerDutyConfig?: boolean };

export type ToggleUsageStatsReportMutationVariables = Exact<{
  toggleDataChoices: ToggleDataChoicesDtoInput;
}>;


export type ToggleUsageStatsReportMutation = { __typename?: 'Mutation', toggleUsageStatsReport?: boolean };

export type UsageStatsReportQueryVariables = Exact<{ [key: string]: never; }>;


export type UsageStatsReportQuery = { __typename?: 'Query', usageStatsReport?: { __typename?: 'UsageStatisticsReportDTO', deviceTypeCounts?: any, monitoredServices: any, nodes: any, systemId?: string, version?: string } };

export type NodeTablePartsFragment = { __typename?: 'Query', findAllNodes?: Array<{ __typename?: 'Node', id: any, nodeLabel?: string, tenantId?: string, createTime: any, monitoringLocationId: any }> };

export type MinionsTablePartsFragment = { __typename?: 'Query', listMinions?: { __typename?: 'MinionCollectionDTO', minions?: Array<{ __typename?: 'MinionDTO', id?: string, status?: string, location?: string, lastUpdated?: any }> } };

export type LocationsPartsFragment = { __typename?: 'Query', listLocations?: { __typename?: 'LocationCollectionDTO', locations?: Array<{ __typename?: 'LocationDTO', locationName?: string }> } };

export type ListNodesForTableQueryVariables = Exact<{ [key: string]: never; }>;


export type ListNodesForTableQuery = { __typename?: 'Query', findAllNodes?: Array<{ __typename?: 'Node', id: any, nodeLabel?: string, tenantId?: string, createTime: any, monitoringLocationId: any }>, deviceUptime?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } }, deviceLatency?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } } };

export type ListMinionsForTableQueryVariables = Exact<{ [key: string]: never; }>;


export type ListMinionsForTableQuery = { __typename?: 'Query', listMinions?: { __typename?: 'MinionCollectionDTO', minions?: Array<{ __typename?: 'MinionDTO', id?: string, status?: string, location?: string, lastUpdated?: any }> }, minionUptime?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } }, minionLatency?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } } };

export type ListMinionsAndDevicesForTablesQueryVariables = Exact<{ [key: string]: never; }>;


export type ListMinionsAndDevicesForTablesQuery = { __typename?: 'Query', findAllNodes?: Array<{ __typename?: 'Node', id: any, nodeLabel?: string, tenantId?: string, createTime: any, monitoringLocationId: any }>, deviceUptime?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } }, deviceLatency?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } }, listMinions?: { __typename?: 'MinionCollectionDTO', minions?: Array<{ __typename?: 'MinionDTO', id?: string, status?: string, location?: string, lastUpdated?: any }> }, minionUptime?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } }, minionLatency?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } }, listLocations?: { __typename?: 'LocationCollectionDTO', locations?: Array<{ __typename?: 'LocationDTO', locationName?: string }> } };

export type GetMetricQueryVariables = Exact<{
  metric: Scalars['String'];
}>;


export type GetMetricQuery = { __typename?: 'Query', metric?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } } };

export type GetTimeSeriesMetricQueryVariables = Exact<{
  name: Scalars['String'];
  monitor?: InputMaybe<Scalars['String']>;
  timeRange: Scalars['Int'];
  timeRangeUnit: TimeRangeUnit;
}>;


export type GetTimeSeriesMetricQuery = { __typename?: 'Query', metric?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, values?: Array<Array<number>> }> } } };

export type NodesListQueryVariables = Exact<{ [key: string]: never; }>;


export type NodesListQuery = { __typename?: 'Query', findAllNodes?: Array<{ __typename?: 'Node', createTime: any, id: any, monitoringLocationId: any, nodeLabel?: string, tenantId?: string, ipInterfaces?: Array<{ __typename?: 'IpInterface', id: any, ipAddress?: string, nodeId: any, tenantId?: string }>, location?: { __typename?: 'Location', id: any, location?: string, tenantId?: string } }> };

export type NodeLatencyMetricQueryVariables = Exact<{
  id: Scalars['Long'];
}>;


export type NodeLatencyMetricQuery = { __typename?: 'Query', nodeLatency?: { __typename?: 'TimeSeriesQueryResult', status?: string, data?: { __typename?: 'TSData', resultType?: string, result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number>, values?: Array<Array<number>> }> } } };

export type NodesForMapQueryVariables = Exact<{ [key: string]: never; }>;


export type NodesForMapQuery = { __typename?: 'Query', findAllNodes?: Array<{ __typename?: 'Node', id: any, nodeLabel?: string }> };

export type EventsByNodeIdPartsFragment = { __typename?: 'Query', events?: Array<{ __typename?: 'Event', id: number, uei?: string, nodeId: number, ipAddress?: string, producedTime: any }> };

export type NodeByIdPartsFragment = { __typename?: 'Query', node?: { __typename?: 'Node', nodeLabel?: string } };

export type ListNodeStatusQueryVariables = Exact<{
  id?: InputMaybe<Scalars['Long']>;
}>;


export type ListNodeStatusQuery = { __typename?: 'Query', events?: Array<{ __typename?: 'Event', id: number, uei?: string, nodeId: number, ipAddress?: string, producedTime: any }>, node?: { __typename?: 'Node', nodeLabel?: string } };

export const ChartPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"ChartParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"TimeSeriesQueryResult"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"data"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"result"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"metric"}},{"kind":"Field","name":{"kind":"Name","value":"values"}}]}}]}}]}}]} as unknown as DocumentNode<ChartPartsFragment, unknown>;
export const ChartTimeSeriesMetricFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"ChartTimeSeriesMetric"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"metric"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"Variable","name":{"kind":"Name","value":"name"}}},{"kind":"Argument","name":{"kind":"Name","value":"labels"},"value":{"kind":"ObjectValue","fields":[{"kind":"ObjectField","name":{"kind":"Name","value":"monitor"},"value":{"kind":"Variable","name":{"kind":"Name","value":"monitor"}}}]}},{"kind":"Argument","name":{"kind":"Name","value":"timeRange"},"value":{"kind":"Variable","name":{"kind":"Name","value":"timeRange"}}},{"kind":"Argument","name":{"kind":"Name","value":"timeRangeUnit"},"value":{"kind":"Variable","name":{"kind":"Name","value":"timeRangeUnit"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"ChartParts"}}]}}]}}]} as unknown as DocumentNode<ChartTimeSeriesMetricFragment, unknown>;
export const MetricPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"MetricParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"TimeSeriesQueryResult"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"data"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"result"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"metric"}},{"kind":"Field","name":{"kind":"Name","value":"value"}}]}}]}}]}}]} as unknown as DocumentNode<MetricPartsFragment, unknown>;
export const DeviceUptimePartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"DeviceUptimeParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"deviceUptime"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"StringValue","value":"snmp_uptime_sec","block":false}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MetricParts"}}]}}]}}]} as unknown as DocumentNode<DeviceUptimePartsFragment, unknown>;
export const DeviceLatencyPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"DeviceLatencyParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"deviceLatency"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"StringValue","value":"response_time_msec","block":false}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MetricParts"}}]}}]}}]} as unknown as DocumentNode<DeviceLatencyPartsFragment, unknown>;
export const TimeSeriesMetricFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"TimeSeriesMetric"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"metric"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"Variable","name":{"kind":"Name","value":"metric"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MetricParts"}}]}}]}}]} as unknown as DocumentNode<TimeSeriesMetricFragment, unknown>;
export const MinionUptimePartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"MinionUptimeParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"minionUptime"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"StringValue","value":"minion_uptime_sec","block":false}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MetricParts"}}]}}]}}]} as unknown as DocumentNode<MinionUptimePartsFragment, unknown>;
export const MinionLatencyPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"MinionLatencyParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"minionLatency"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"StringValue","value":"minion_response_time_msec","block":false}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MetricParts"}}]}}]}}]} as unknown as DocumentNode<MinionLatencyPartsFragment, unknown>;
export const NodesUptimePartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"NodesUptimeParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"nodesUptime"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"StringValue","value":"snmp_uptime_sec","block":false}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MetricParts"}}]}}]}}]} as unknown as DocumentNode<NodesUptimePartsFragment, unknown>;
export const NodesLatencyPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"NodesLatencyParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"nodesLatency"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"StringValue","value":"response_time_msec","block":false}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MetricParts"}}]}}]}}]} as unknown as DocumentNode<NodesLatencyPartsFragment, unknown>;
export const NodeLatencyPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"NodeLatencyParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"nodeLatency"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"StringValue","value":"response_time_msec","block":false}},{"kind":"Argument","name":{"kind":"Name","value":"labels"},"value":{"kind":"ObjectValue","fields":[{"kind":"ObjectField","name":{"kind":"Name","value":"node_id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}}]}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"status"}},{"kind":"Field","name":{"kind":"Name","value":"data"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"result"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"metric"}},{"kind":"Field","name":{"kind":"Name","value":"value"}},{"kind":"Field","name":{"kind":"Name","value":"values"}}]}},{"kind":"Field","name":{"kind":"Name","value":"resultType"}}]}}]}}]}}]} as unknown as DocumentNode<NodeLatencyPartsFragment, unknown>;
export const NodesPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"NodesParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"findAllNodes"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"createTime"}},{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"ipInterfaces"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"ipAddress"}},{"kind":"Field","name":{"kind":"Name","value":"nodeId"}},{"kind":"Field","name":{"kind":"Name","value":"tenantId"}}]}},{"kind":"Field","name":{"kind":"Name","value":"location"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"location"}},{"kind":"Field","name":{"kind":"Name","value":"tenantId"}}]}},{"kind":"Field","name":{"kind":"Name","value":"monitoringLocationId"}},{"kind":"Field","name":{"kind":"Name","value":"nodeLabel"}},{"kind":"Field","name":{"kind":"Name","value":"tenantId"}}]}}]}}]} as unknown as DocumentNode<NodesPartsFragment, unknown>;
export const NodeTablePartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"NodeTableParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"findAllNodes"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"nodeLabel"}},{"kind":"Field","name":{"kind":"Name","value":"tenantId"}},{"kind":"Field","name":{"kind":"Name","value":"createTime"}},{"kind":"Field","name":{"kind":"Name","value":"monitoringLocationId"}}]}}]}}]} as unknown as DocumentNode<NodeTablePartsFragment, unknown>;
export const MinionsTablePartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"MinionsTableParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"listMinions"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"minions"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"status"}},{"kind":"Field","name":{"kind":"Name","value":"location"}},{"kind":"Field","name":{"kind":"Name","value":"lastUpdated"}}]}}]}}]}}]} as unknown as DocumentNode<MinionsTablePartsFragment, unknown>;
export const LocationsPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"LocationsParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"listLocations"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"locations"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"locationName"}}]}}]}}]}}]} as unknown as DocumentNode<LocationsPartsFragment, unknown>;
export const EventsByNodeIdPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"EventsByNodeIdParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"events"},"name":{"kind":"Name","value":"findEventsByNodeId"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"uei"}},{"kind":"Field","name":{"kind":"Name","value":"nodeId"}},{"kind":"Field","name":{"kind":"Name","value":"ipAddress"}},{"kind":"Field","name":{"kind":"Name","value":"producedTime"}}]}}]}}]} as unknown as DocumentNode<EventsByNodeIdPartsFragment, unknown>;
export const NodeByIdPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"NodeByIdParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"node"},"name":{"kind":"Name","value":"findNodeById"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"nodeLabel"}}]}}]}}]} as unknown as DocumentNode<NodeByIdPartsFragment, unknown>;
export const AlarmsDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"Alarms"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"listAlarms"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"alarms"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"description"}},{"kind":"Field","name":{"kind":"Name","value":"severity"}},{"kind":"Field","name":{"kind":"Name","value":"lastEventTime"}}]}}]}}]}}]} as unknown as DocumentNode<AlarmsQuery, AlarmsQueryVariables>;
export const ClearAlarmDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"ClearAlarm"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"id"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"ackDTO"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"AlarmAckDTOInput"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"clearAlarm"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}},{"kind":"Argument","name":{"kind":"Name","value":"ackDTO"},"value":{"kind":"Variable","name":{"kind":"Name","value":"ackDTO"}}}]}]}}]} as unknown as DocumentNode<ClearAlarmMutation, ClearAlarmMutationVariables>;
export const CreateEventDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"CreateEvent"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"event"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"EventDTOInput"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"createEvent"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"event"},"value":{"kind":"Variable","name":{"kind":"Name","value":"event"}}}]}]}}]} as unknown as DocumentNode<CreateEventMutation, CreateEventMutationVariables>;
export const AddNodeDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"AddNode"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"node"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"NodeCreateInput"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"addNode"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"node"},"value":{"kind":"Variable","name":{"kind":"Name","value":"node"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"createTime"}},{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"monitoringLocationId"}},{"kind":"Field","name":{"kind":"Name","value":"nodeLabel"}},{"kind":"Field","name":{"kind":"Name","value":"tenantId"}}]}}]}}]} as unknown as DocumentNode<AddNodeMutation, AddNodeMutationVariables>;
export const SavePagerDutyConfigDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"SavePagerDutyConfig"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"config"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"PagerDutyConfigDTOInput"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"savePagerDutyConfig"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"config"},"value":{"kind":"Variable","name":{"kind":"Name","value":"config"}}}]}]}}]} as unknown as DocumentNode<SavePagerDutyConfigMutation, SavePagerDutyConfigMutationVariables>;
export const ToggleUsageStatsReportDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"ToggleUsageStatsReport"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"toggleDataChoices"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"ToggleDataChoicesDTOInput"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"toggleUsageStatsReport"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"toggleDataChoices"},"value":{"kind":"Variable","name":{"kind":"Name","value":"toggleDataChoices"}}}]}]}}]} as unknown as DocumentNode<ToggleUsageStatsReportMutation, ToggleUsageStatsReportMutationVariables>;
export const UsageStatsReportDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"UsageStatsReport"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"usageStatsReport"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"deviceTypeCounts"}},{"kind":"Field","name":{"kind":"Name","value":"monitoredServices"}},{"kind":"Field","name":{"kind":"Name","value":"nodes"}},{"kind":"Field","name":{"kind":"Name","value":"systemId"}},{"kind":"Field","name":{"kind":"Name","value":"version"}}]}}]}}]} as unknown as DocumentNode<UsageStatsReportQuery, UsageStatsReportQueryVariables>;
export const ListNodesForTableDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"ListNodesForTable"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"NodeTableParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"DeviceUptimeParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"DeviceLatencyParts"}}]}},...NodeTablePartsFragmentDoc.definitions,...DeviceUptimePartsFragmentDoc.definitions,...MetricPartsFragmentDoc.definitions,...DeviceLatencyPartsFragmentDoc.definitions]} as unknown as DocumentNode<ListNodesForTableQuery, ListNodesForTableQueryVariables>;
export const ListMinionsForTableDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"ListMinionsForTable"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MinionsTableParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"MinionUptimeParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"MinionLatencyParts"}}]}},...MinionsTablePartsFragmentDoc.definitions,...MinionUptimePartsFragmentDoc.definitions,...MetricPartsFragmentDoc.definitions,...MinionLatencyPartsFragmentDoc.definitions]} as unknown as DocumentNode<ListMinionsForTableQuery, ListMinionsForTableQueryVariables>;
export const ListMinionsAndDevicesForTablesDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"ListMinionsAndDevicesForTables"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"NodeTableParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"DeviceUptimeParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"DeviceLatencyParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"MinionsTableParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"MinionUptimeParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"MinionLatencyParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"LocationsParts"}}]}},...NodeTablePartsFragmentDoc.definitions,...DeviceUptimePartsFragmentDoc.definitions,...MetricPartsFragmentDoc.definitions,...DeviceLatencyPartsFragmentDoc.definitions,...MinionsTablePartsFragmentDoc.definitions,...MinionUptimePartsFragmentDoc.definitions,...MinionLatencyPartsFragmentDoc.definitions,...LocationsPartsFragmentDoc.definitions]} as unknown as DocumentNode<ListMinionsAndDevicesForTablesQuery, ListMinionsAndDevicesForTablesQueryVariables>;
export const GetMetricDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"GetMetric"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"metric"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"TimeSeriesMetric"}}]}},...TimeSeriesMetricFragmentDoc.definitions,...MetricPartsFragmentDoc.definitions]} as unknown as DocumentNode<GetMetricQuery, GetMetricQueryVariables>;
export const GetTimeSeriesMetricDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"GetTimeSeriesMetric"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"name"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"monitor"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"timeRange"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Int"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"timeRangeUnit"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"TimeRangeUnit"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"ChartTimeSeriesMetric"}}]}},...ChartTimeSeriesMetricFragmentDoc.definitions,...ChartPartsFragmentDoc.definitions]} as unknown as DocumentNode<GetTimeSeriesMetricQuery, GetTimeSeriesMetricQueryVariables>;
export const NodesListDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"NodesList"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"NodesParts"}}]}},...NodesPartsFragmentDoc.definitions]} as unknown as DocumentNode<NodesListQuery, NodesListQueryVariables>;
export const NodeLatencyMetricDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"NodeLatencyMetric"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"id"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"NodeLatencyParts"}}]}},...NodeLatencyPartsFragmentDoc.definitions]} as unknown as DocumentNode<NodeLatencyMetricQuery, NodeLatencyMetricQueryVariables>;
export const NodesForMapDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"NodesForMap"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"findAllNodes"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"nodeLabel"}}]}}]}}]} as unknown as DocumentNode<NodesForMapQuery, NodesForMapQueryVariables>;
export const ListNodeStatusDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"ListNodeStatus"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"id"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"EventsByNodeIdParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"NodeByIdParts"}}]}},...EventsByNodeIdPartsFragmentDoc.definitions,...NodeByIdPartsFragmentDoc.definitions]} as unknown as DocumentNode<ListNodeStatusQuery, ListNodeStatusQueryVariables>;