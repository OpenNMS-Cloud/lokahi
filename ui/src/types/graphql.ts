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
  offset?: Maybe<Scalars['Int']>;
  totalCount?: Maybe<Scalars['Int']>;
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
  offset?: Maybe<Scalars['Int']>;
  totalCount?: Maybe<Scalars['Int']>;
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

export type EventCollectionDto = {
  __typename?: 'EventCollectionDTO';
  events?: Maybe<Array<Maybe<EventDto>>>;
  offset?: Maybe<Scalars['Int']>;
  totalCount?: Maybe<Scalars['Int']>;
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

export type LocationCollectionDto = {
  __typename?: 'LocationCollectionDTO';
  locations?: Maybe<Array<Maybe<LocationDto>>>;
  offset?: Maybe<Scalars['Int']>;
  totalCount?: Maybe<Scalars['Int']>;
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

export type MinionCollectionDto = {
  __typename?: 'MinionCollectionDTO';
  minions?: Maybe<Array<Maybe<MinionDto>>>;
  offset?: Maybe<Scalars['Int']>;
  totalCount?: Maybe<Scalars['Int']>;
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

export type PagerDutyConfigDtoInput = {
  integrationkey?: InputMaybe<Scalars['String']>;
};

/** Query root */
export type Query = {
  __typename?: 'Query';
  deviceById?: Maybe<DeviceDto>;
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
export type QueryLocationByIdArgs = {
  id?: InputMaybe<Scalars['String']>;
};


/** Query root */
export type QueryMetricArgs = {
  labels?: InputMaybe<Scalars['Map_String_StringScalar']>;
  name?: InputMaybe<Scalars['String']>;
};


/** Query root */
export type QueryMinionByIdArgs = {
  id?: InputMaybe<Scalars['String']>;
};

export type ReductionKeyMemoDto = {
  __typename?: 'ReductionKeyMemoDTO';
  author?: Maybe<Scalars['String']>;
  body?: Maybe<Scalars['String']>;
  created?: Maybe<Scalars['Date']>;
  id?: Maybe<Scalars['Int']>;
  reductionKey?: Maybe<Scalars['String']>;
  updated?: Maybe<Scalars['Date']>;
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

export type TsData = {
  __typename?: 'TSData';
  result?: Maybe<Array<Maybe<TsResult>>>;
  resultType?: Maybe<Scalars['String']>;
};

export type TsResult = {
  __typename?: 'TSResult';
  metric?: Maybe<Scalars['Map_String_StringScalar']>;
  value?: Maybe<Array<Maybe<Scalars['Float']>>>;
};

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

export type AddDeviceMutationVariables = Exact<{
  device: DeviceCreateDtoInput;
}>;


export type AddDeviceMutation = { __typename?: 'Mutation', addDevice?: number };

export type CreateEventMutationVariables = Exact<{
  event: EventDtoInput;
}>;


export type CreateEventMutation = { __typename?: 'Mutation', createEvent?: boolean };

export type DeviceUptimePartsFragment = { __typename?: 'Query', deviceUptime?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } } };

export type DeviceLatencyPartsFragment = { __typename?: 'Query', deviceLatency?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } } };

export type MetricPartsFragment = { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } };

export type TimeSeriesMetricFragment = { __typename?: 'Query', metric?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } } };

export type MinionUptimePartsFragment = { __typename?: 'Query', minionUptime?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } } };

export type MinionLatencyPartsFragment = { __typename?: 'Query', minionLatency?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } } };

export type SavePagerDutyConfigMutationVariables = Exact<{
  config: PagerDutyConfigDtoInput;
}>;


export type SavePagerDutyConfigMutation = { __typename?: 'Mutation', savePagerDutyConfig?: boolean };

export type DevicesTablePartsFragment = { __typename?: 'Query', listDevices?: { __typename?: 'DeviceCollectionDTO', devices?: Array<{ __typename?: 'DeviceDTO', id?: number, label?: string, createTime?: any, managementIp?: string }> } };

export type MinionsTablePartsFragment = { __typename?: 'Query', listMinions?: { __typename?: 'MinionCollectionDTO', minions?: Array<{ __typename?: 'MinionDTO', id?: string, status?: string, location?: string, lastUpdated?: any }> } };

export type LocationsPartsFragment = { __typename?: 'Query', listLocations?: { __typename?: 'LocationCollectionDTO', locations?: Array<{ __typename?: 'LocationDTO', locationName?: string }> } };

export type ListDevicesForTableQueryVariables = Exact<{ [key: string]: never; }>;


export type ListDevicesForTableQuery = { __typename?: 'Query', listDevices?: { __typename?: 'DeviceCollectionDTO', devices?: Array<{ __typename?: 'DeviceDTO', id?: number, label?: string, createTime?: any, managementIp?: string }> }, deviceUptime?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } }, deviceLatency?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } } };

export type ListMinionsForTableQueryVariables = Exact<{ [key: string]: never; }>;


export type ListMinionsForTableQuery = { __typename?: 'Query', listMinions?: { __typename?: 'MinionCollectionDTO', minions?: Array<{ __typename?: 'MinionDTO', id?: string, status?: string, location?: string, lastUpdated?: any }> }, minionUptime?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } }, minionLatency?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } } };

export type ListMinionsAndDevicesForTablesQueryVariables = Exact<{ [key: string]: never; }>;


export type ListMinionsAndDevicesForTablesQuery = { __typename?: 'Query', listDevices?: { __typename?: 'DeviceCollectionDTO', devices?: Array<{ __typename?: 'DeviceDTO', id?: number, label?: string, createTime?: any, managementIp?: string }> }, deviceUptime?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } }, deviceLatency?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } }, listMinions?: { __typename?: 'MinionCollectionDTO', minions?: Array<{ __typename?: 'MinionDTO', id?: string, status?: string, location?: string, lastUpdated?: any }> }, minionUptime?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } }, minionLatency?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } }, listLocations?: { __typename?: 'LocationCollectionDTO', locations?: Array<{ __typename?: 'LocationDTO', locationName?: string }> } };

export type GetMetricQueryVariables = Exact<{
  metric: Scalars['String'];
}>;


export type GetMetricQuery = { __typename?: 'Query', metric?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } } };

export type DeviceForMapQueryVariables = Exact<{ [key: string]: never; }>;


export type DeviceForMapQuery = { __typename?: 'Query', listDevices?: { __typename?: 'DeviceCollectionDTO', devices?: Array<{ __typename?: 'DeviceDTO', foreignId?: string, foreignSource?: string, id?: number, label?: string, labelSource?: string, sysContact?: string, sysDescription?: string, sysLocation?: string, sysName?: string, sysOid?: string, location?: { __typename?: 'LocationDTO', latitude?: number, longitude?: number } }> } };

export type ListEventsPartsFragment = { __typename?: 'Query', listEvents?: { __typename?: 'EventCollectionDTO', offset?: number, totalCount?: number, events?: Array<{ __typename?: 'EventDTO', id?: number, severity?: string, time?: any, source?: string, nodeLabel?: string, location?: string, ipAddress?: string, nodeId?: number }> } };

export type DevicePartsFragment = { __typename?: 'DeviceDTO', foreignSource?: string, managementIp?: string, id?: number, label?: string, createTime?: any, location?: { __typename?: 'LocationDTO', locationName?: string, latitude?: number, longitude?: number } };

export type DeviceByIdPartsFragment = { __typename?: 'Query', device?: { __typename?: 'DeviceDTO', foreignSource?: string, managementIp?: string, id?: number, label?: string, createTime?: any, location?: { __typename?: 'LocationDTO', locationName?: string, latitude?: number, longitude?: number } } };

export type ListDeviceStatusQueryVariables = Exact<{
  id?: InputMaybe<Scalars['Int']>;
}>;


export type ListDeviceStatusQuery = { __typename?: 'Query', listEvents?: { __typename?: 'EventCollectionDTO', offset?: number, totalCount?: number, events?: Array<{ __typename?: 'EventDTO', id?: number, severity?: string, time?: any, source?: string, nodeLabel?: string, location?: string, ipAddress?: string, nodeId?: number }> }, device?: { __typename?: 'DeviceDTO', foreignSource?: string, managementIp?: string, id?: number, label?: string, createTime?: any, location?: { __typename?: 'LocationDTO', locationName?: string, latitude?: number, longitude?: number } }, deviceUptime?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } }, deviceLatency?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number> }> } } };

export const MetricPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"MetricParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"TimeSeriesQueryResult"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"data"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"result"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"metric"}},{"kind":"Field","name":{"kind":"Name","value":"value"}}]}}]}}]}}]} as unknown as DocumentNode<MetricPartsFragment, unknown>;
export const DeviceUptimePartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"DeviceUptimeParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"deviceUptime"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"StringValue","value":"snmp_uptime_sec","block":false}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MetricParts"}}]}}]}}]} as unknown as DocumentNode<DeviceUptimePartsFragment, unknown>;
export const DeviceLatencyPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"DeviceLatencyParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"deviceLatency"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"StringValue","value":"icmp_round_trip_time_msec","block":false}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MetricParts"}}]}}]}}]} as unknown as DocumentNode<DeviceLatencyPartsFragment, unknown>;
export const TimeSeriesMetricFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"TimeSeriesMetric"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"metric"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"Variable","name":{"kind":"Name","value":"metric"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MetricParts"}}]}}]}}]} as unknown as DocumentNode<TimeSeriesMetricFragment, unknown>;
export const MinionUptimePartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"MinionUptimeParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"minionUptime"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"StringValue","value":"minion_uptime_sec","block":false}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MetricParts"}}]}}]}}]} as unknown as DocumentNode<MinionUptimePartsFragment, unknown>;
export const MinionLatencyPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"MinionLatencyParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"minionLatency"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"StringValue","value":"minion_response_time_msec","block":false}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MetricParts"}}]}}]}}]} as unknown as DocumentNode<MinionLatencyPartsFragment, unknown>;
export const DevicesTablePartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"DevicesTableParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"listDevices"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"devices"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"label"}},{"kind":"Field","name":{"kind":"Name","value":"createTime"}},{"kind":"Field","name":{"kind":"Name","value":"managementIp"}}]}}]}}]}}]} as unknown as DocumentNode<DevicesTablePartsFragment, unknown>;
export const MinionsTablePartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"MinionsTableParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"listMinions"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"minions"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"status"}},{"kind":"Field","name":{"kind":"Name","value":"location"}},{"kind":"Field","name":{"kind":"Name","value":"lastUpdated"}}]}}]}}]}}]} as unknown as DocumentNode<MinionsTablePartsFragment, unknown>;
export const LocationsPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"LocationsParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"listLocations"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"locations"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"locationName"}}]}}]}}]}}]} as unknown as DocumentNode<LocationsPartsFragment, unknown>;
export const ListEventsPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"ListEventsParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"listEvents"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"events"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"severity"}},{"kind":"Field","name":{"kind":"Name","value":"time"}},{"kind":"Field","name":{"kind":"Name","value":"source"}},{"kind":"Field","name":{"kind":"Name","value":"nodeLabel"}},{"kind":"Field","name":{"kind":"Name","value":"location"}},{"kind":"Field","name":{"kind":"Name","value":"ipAddress"}},{"kind":"Field","name":{"kind":"Name","value":"nodeId"}}]}},{"kind":"Field","name":{"kind":"Name","value":"offset"}},{"kind":"Field","name":{"kind":"Name","value":"totalCount"}}]}}]}}]} as unknown as DocumentNode<ListEventsPartsFragment, unknown>;
export const DevicePartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"DeviceParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"DeviceDTO"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"foreignSource"}},{"kind":"Field","name":{"kind":"Name","value":"managementIp"}},{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"label"}},{"kind":"Field","name":{"kind":"Name","value":"createTime"}},{"kind":"Field","name":{"kind":"Name","value":"location"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"locationName"}},{"kind":"Field","name":{"kind":"Name","value":"latitude"}},{"kind":"Field","name":{"kind":"Name","value":"longitude"}}]}}]}}]} as unknown as DocumentNode<DevicePartsFragment, unknown>;
export const DeviceByIdPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"DeviceByIdParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"device"},"name":{"kind":"Name","value":"deviceById"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"DeviceParts"}}]}}]}}]} as unknown as DocumentNode<DeviceByIdPartsFragment, unknown>;
export const AlarmsDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"Alarms"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"listAlarms"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"alarms"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"description"}},{"kind":"Field","name":{"kind":"Name","value":"severity"}},{"kind":"Field","name":{"kind":"Name","value":"lastEventTime"}}]}}]}}]}}]} as unknown as DocumentNode<AlarmsQuery, AlarmsQueryVariables>;
export const ClearAlarmDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"ClearAlarm"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"id"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"ackDTO"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"AlarmAckDTOInput"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"clearAlarm"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}},{"kind":"Argument","name":{"kind":"Name","value":"ackDTO"},"value":{"kind":"Variable","name":{"kind":"Name","value":"ackDTO"}}}]}]}}]} as unknown as DocumentNode<ClearAlarmMutation, ClearAlarmMutationVariables>;
export const AddDeviceDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"AddDevice"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"device"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"DeviceCreateDTOInput"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"addDevice"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"device"},"value":{"kind":"Variable","name":{"kind":"Name","value":"device"}}}]}]}}]} as unknown as DocumentNode<AddDeviceMutation, AddDeviceMutationVariables>;
export const CreateEventDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"CreateEvent"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"event"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"EventDTOInput"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"createEvent"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"event"},"value":{"kind":"Variable","name":{"kind":"Name","value":"event"}}}]}]}}]} as unknown as DocumentNode<CreateEventMutation, CreateEventMutationVariables>;
export const SavePagerDutyConfigDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"SavePagerDutyConfig"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"config"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"PagerDutyConfigDTOInput"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"savePagerDutyConfig"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"config"},"value":{"kind":"Variable","name":{"kind":"Name","value":"config"}}}]}]}}]} as unknown as DocumentNode<SavePagerDutyConfigMutation, SavePagerDutyConfigMutationVariables>;
export const ListDevicesForTableDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"ListDevicesForTable"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"DevicesTableParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"DeviceUptimeParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"DeviceLatencyParts"}}]}},...DevicesTablePartsFragmentDoc.definitions,...DeviceUptimePartsFragmentDoc.definitions,...MetricPartsFragmentDoc.definitions,...DeviceLatencyPartsFragmentDoc.definitions]} as unknown as DocumentNode<ListDevicesForTableQuery, ListDevicesForTableQueryVariables>;
export const ListMinionsForTableDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"ListMinionsForTable"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MinionsTableParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"MinionUptimeParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"MinionLatencyParts"}}]}},...MinionsTablePartsFragmentDoc.definitions,...MinionUptimePartsFragmentDoc.definitions,...MetricPartsFragmentDoc.definitions,...MinionLatencyPartsFragmentDoc.definitions]} as unknown as DocumentNode<ListMinionsForTableQuery, ListMinionsForTableQueryVariables>;
export const ListMinionsAndDevicesForTablesDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"ListMinionsAndDevicesForTables"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"DevicesTableParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"DeviceUptimeParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"DeviceLatencyParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"MinionsTableParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"MinionUptimeParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"MinionLatencyParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"LocationsParts"}}]}},...DevicesTablePartsFragmentDoc.definitions,...DeviceUptimePartsFragmentDoc.definitions,...MetricPartsFragmentDoc.definitions,...DeviceLatencyPartsFragmentDoc.definitions,...MinionsTablePartsFragmentDoc.definitions,...MinionUptimePartsFragmentDoc.definitions,...MinionLatencyPartsFragmentDoc.definitions,...LocationsPartsFragmentDoc.definitions]} as unknown as DocumentNode<ListMinionsAndDevicesForTablesQuery, ListMinionsAndDevicesForTablesQueryVariables>;
export const GetMetricDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"GetMetric"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"metric"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"TimeSeriesMetric"}}]}},...TimeSeriesMetricFragmentDoc.definitions,...MetricPartsFragmentDoc.definitions]} as unknown as DocumentNode<GetMetricQuery, GetMetricQueryVariables>;
export const DeviceForMapDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"DeviceForMap"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"listDevices"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"devices"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"foreignId"}},{"kind":"Field","name":{"kind":"Name","value":"foreignSource"}},{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"label"}},{"kind":"Field","name":{"kind":"Name","value":"labelSource"}},{"kind":"Field","name":{"kind":"Name","value":"location"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"latitude"}},{"kind":"Field","name":{"kind":"Name","value":"longitude"}}]}},{"kind":"Field","name":{"kind":"Name","value":"sysContact"}},{"kind":"Field","name":{"kind":"Name","value":"sysDescription"}},{"kind":"Field","name":{"kind":"Name","value":"sysLocation"}},{"kind":"Field","name":{"kind":"Name","value":"sysName"}},{"kind":"Field","name":{"kind":"Name","value":"sysOid"}}]}}]}}]}}]} as unknown as DocumentNode<DeviceForMapQuery, DeviceForMapQueryVariables>;
export const ListDeviceStatusDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"ListDeviceStatus"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"id"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"Int"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"ListEventsParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"DeviceByIdParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"DeviceUptimeParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"DeviceLatencyParts"}}]}},...ListEventsPartsFragmentDoc.definitions,...DeviceByIdPartsFragmentDoc.definitions,...DevicePartsFragmentDoc.definitions,...DeviceUptimePartsFragmentDoc.definitions,...MetricPartsFragmentDoc.definitions,...DeviceLatencyPartsFragmentDoc.definitions]} as unknown as DocumentNode<ListDeviceStatusQuery, ListDeviceStatusQueryVariables>;