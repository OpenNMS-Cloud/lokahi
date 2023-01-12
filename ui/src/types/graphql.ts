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
  Long: any;
  Map_String_StringScalar: any;
  UNREPRESENTABLE: any;
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

export type Minion = {
  __typename?: 'Minion';
  id: Scalars['Long'];
  label?: Maybe<Scalars['String']>;
  lastCheckedTime: Scalars['Long'];
  location?: Maybe<Location>;
  locationId: Scalars['Long'];
  status?: Maybe<Scalars['String']>;
  systemId?: Maybe<Scalars['String']>;
  tenantId?: Maybe<Scalars['String']>;
};

/** Mutation root */
export type Mutation = {
  __typename?: 'Mutation';
  addNode?: Maybe<Node>;
  savePagerDutyConfig?: Maybe<Scalars['Boolean']>;
};


/** Mutation root */
export type MutationAddNodeArgs = {
  node?: InputMaybe<NodeCreateInput>;
};


/** Mutation root */
export type MutationSavePagerDutyConfigArgs = {
  config?: InputMaybe<PagerDutyConfigDtoInput>;
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

export type NodeStatus = {
  __typename?: 'NodeStatus';
  id: Scalars['Long'];
  status?: Maybe<Scalars['String']>;
};

export type PagerDutyConfigDtoInput = {
  integrationkey?: InputMaybe<Scalars['String']>;
};

/** Query root */
export type Query = {
  __typename?: 'Query';
  findAllEvents?: Maybe<Array<Maybe<Event>>>;
  findAllLocations?: Maybe<Array<Maybe<Location>>>;
  findAllMinions?: Maybe<Array<Maybe<Minion>>>;
  findAllNodes?: Maybe<Array<Maybe<Node>>>;
  findEventsByNodeId?: Maybe<Array<Maybe<Event>>>;
  findLocationById?: Maybe<Location>;
  findMinionById?: Maybe<Minion>;
  findNodeById?: Maybe<Node>;
  metric?: Maybe<TimeSeriesQueryResult>;
  nodeStatus?: Maybe<NodeStatus>;
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
export type QueryMetricArgs = {
  labels?: InputMaybe<Scalars['Map_String_StringScalar']>;
  name?: InputMaybe<Scalars['String']>;
  timeRange?: InputMaybe<Scalars['Int']>;
  timeRangeUnit?: InputMaybe<TimeRangeUnit>;
};


/** Query root */
export type QueryNodeStatusArgs = {
  id?: InputMaybe<Scalars['Long']>;
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

export type ChartPartsFragment = { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, values?: Array<Array<number>> }> } };

export type ChartTimeSeriesMetricFragment = { __typename?: 'Query', metric?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, values?: Array<Array<number>> }> } } };

export type DeviceUptimePartsFragment = { __typename?: 'Query', deviceUptime?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number>, values?: Array<Array<number>> }> } } };

export type DeviceLatencyPartsFragment = { __typename?: 'Query', deviceLatency?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number>, values?: Array<Array<number>> }> } } };

export type MetricPartsFragment = { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number>, values?: Array<Array<number>> }> } };

export type TimeSeriesMetricFragment = { __typename?: 'Query', metric?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number>, values?: Array<Array<number>> }> } } };

export type MinionUptimePartsFragment = { __typename?: 'Query', minionUptime?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number>, values?: Array<Array<number>> }> } } };

export type MinionLatencyPartsFragment = { __typename?: 'Query', minionLatency?: { __typename?: 'TimeSeriesQueryResult', status?: string, data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number>, values?: Array<Array<number>> }> } } };

export type NodeLatencyPartsFragment = { __typename?: 'Query', nodeLatency?: { __typename?: 'TimeSeriesQueryResult', status?: string, data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number>, values?: Array<Array<number>> }> } } };

export type AddNodeMutationVariables = Exact<{
  node: NodeCreateInput;
}>;


export type AddNodeMutation = { __typename?: 'Mutation', addNode?: { __typename?: 'Node', createTime: any, id: any, monitoringLocationId: any, nodeLabel?: string, tenantId?: string } };

export type NodeStatusPartsFragment = { __typename?: 'Query', nodeStatus?: { __typename?: 'NodeStatus', id: any, status?: string } };

export type NodesPartsFragment = { __typename?: 'Query', findAllNodes?: Array<{ __typename?: 'Node', createTime: any, id: any, monitoringLocationId: any, nodeLabel?: string, tenantId?: string, ipInterfaces?: Array<{ __typename?: 'IpInterface', id: any, ipAddress?: string, nodeId: any, tenantId?: string }>, location?: { __typename?: 'Location', id: any, location?: string, tenantId?: string } }> };

export type SavePagerDutyConfigMutationVariables = Exact<{
  config: PagerDutyConfigDtoInput;
}>;


export type SavePagerDutyConfigMutation = { __typename?: 'Mutation', savePagerDutyConfig?: boolean };

export type AppliancesNodesTablePartsFragment = { __typename?: 'Query', findAllNodes?: Array<{ __typename?: 'Node', id: any, nodeLabel?: string, tenantId?: string, createTime: any, monitoringLocationId: any, ipInterfaces?: Array<{ __typename?: 'IpInterface', ipAddress?: string }> }> };

export type MinionsTablePartsFragment = { __typename?: 'Query', findAllMinions?: Array<{ __typename?: 'Minion', id: any, label?: string, lastCheckedTime: any, status?: string, systemId?: string, location?: { __typename?: 'Location', id: any, location?: string } }> };

export type LocationsPartsFragment = { __typename?: 'Query', findAllLocations?: Array<{ __typename?: 'Location', id: any, location?: string }> };

export type ListNodesForTableQueryVariables = Exact<{ [key: string]: never; }>;


export type ListNodesForTableQuery = { __typename?: 'Query', findAllNodes?: Array<{ __typename?: 'Node', id: any, nodeLabel?: string, tenantId?: string, createTime: any, monitoringLocationId: any, ipInterfaces?: Array<{ __typename?: 'IpInterface', ipAddress?: string }> }> };

export type ListMinionsForTableQueryVariables = Exact<{ [key: string]: never; }>;


export type ListMinionsForTableQuery = { __typename?: 'Query', findAllMinions?: Array<{ __typename?: 'Minion', id: any, label?: string, lastCheckedTime: any, status?: string, systemId?: string, location?: { __typename?: 'Location', id: any, location?: string } }> };

export type ListMinionMetricsQueryVariables = Exact<{
  instance: Scalars['String'];
  monitor: Scalars['String'];
}>;


export type ListMinionMetricsQuery = { __typename?: 'Query', minionLatency?: { __typename?: 'TimeSeriesQueryResult', status?: string, data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number>, values?: Array<Array<number>> }> } } };

export type ListNodeMetricsQueryVariables = Exact<{
  id: Scalars['Long'];
  monitor: Scalars['String'];
  instance: Scalars['String'];
}>;


export type ListNodeMetricsQuery = { __typename?: 'Query', nodeLatency?: { __typename?: 'TimeSeriesQueryResult', status?: string, data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number>, values?: Array<Array<number>> }> } }, nodeStatus?: { __typename?: 'NodeStatus', id: any, status?: string } };

export type ListMinionsAndDevicesForTablesQueryVariables = Exact<{ [key: string]: never; }>;


export type ListMinionsAndDevicesForTablesQuery = { __typename?: 'Query', findAllNodes?: Array<{ __typename?: 'Node', id: any, nodeLabel?: string, tenantId?: string, createTime: any, monitoringLocationId: any, ipInterfaces?: Array<{ __typename?: 'IpInterface', ipAddress?: string }> }>, findAllMinions?: Array<{ __typename?: 'Minion', id: any, label?: string, lastCheckedTime: any, status?: string, systemId?: string, location?: { __typename?: 'Location', id: any, location?: string } }>, findAllLocations?: Array<{ __typename?: 'Location', id: any, location?: string }> };

export type ListLocationsForDiscoveryQueryVariables = Exact<{ [key: string]: never; }>;


export type ListLocationsForDiscoveryQuery = { __typename?: 'Query', findAllLocations?: Array<{ __typename?: 'Location', id: any, location?: string }> };

export type GetMetricQueryVariables = Exact<{
  metric: Scalars['String'];
}>;


export type GetMetricQuery = { __typename?: 'Query', metric?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number>, values?: Array<Array<number>> }> } } };

export type GetTimeSeriesMetricQueryVariables = Exact<{
  name: Scalars['String'];
  monitor: Scalars['String'];
  nodeId?: InputMaybe<Scalars['String']>;
  timeRange: Scalars['Int'];
  timeRangeUnit: TimeRangeUnit;
  instance: Scalars['String'];
}>;


export type GetTimeSeriesMetricQuery = { __typename?: 'Query', metric?: { __typename?: 'TimeSeriesQueryResult', data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, values?: Array<Array<number>> }> } } };

export type GetNodeForGraphsQueryVariables = Exact<{
  id?: InputMaybe<Scalars['Long']>;
}>;


export type GetNodeForGraphsQuery = { __typename?: 'Query', findNodeById?: { __typename?: 'Node', id: any, ipInterfaces?: Array<{ __typename?: 'IpInterface', ipAddress?: string }> } };

export type NodesListQueryVariables = Exact<{ [key: string]: never; }>;


export type NodesListQuery = { __typename?: 'Query', findAllNodes?: Array<{ __typename?: 'Node', createTime: any, id: any, monitoringLocationId: any, nodeLabel?: string, tenantId?: string, ipInterfaces?: Array<{ __typename?: 'IpInterface', id: any, ipAddress?: string, nodeId: any, tenantId?: string }>, location?: { __typename?: 'Location', id: any, location?: string, tenantId?: string } }> };

export type NodeLatencyMetricQueryVariables = Exact<{
  id: Scalars['Long'];
  monitor: Scalars['String'];
  instance: Scalars['String'];
}>;


export type NodeLatencyMetricQuery = { __typename?: 'Query', nodeLatency?: { __typename?: 'TimeSeriesQueryResult', status?: string, data?: { __typename?: 'TSData', result?: Array<{ __typename?: 'TSResult', metric?: any, value?: Array<number>, values?: Array<Array<number>> }> } }, nodeStatus?: { __typename?: 'NodeStatus', id: any, status?: string } };

export type NodesForMapQueryVariables = Exact<{ [key: string]: never; }>;


export type NodesForMapQuery = { __typename?: 'Query', findAllNodes?: Array<{ __typename?: 'Node', id: any, nodeLabel?: string }> };

export type EventsByNodeIdPartsFragment = { __typename?: 'Query', events?: Array<{ __typename?: 'Event', id: number, uei?: string, nodeId: number, ipAddress?: string, producedTime: any }> };

export type NodeByIdPartsFragment = { __typename?: 'Query', node?: { __typename?: 'Node', nodeLabel?: string } };

export type ListNodeStatusQueryVariables = Exact<{
  id?: InputMaybe<Scalars['Long']>;
}>;


export type ListNodeStatusQuery = { __typename?: 'Query', events?: Array<{ __typename?: 'Event', id: number, uei?: string, nodeId: number, ipAddress?: string, producedTime: any }>, node?: { __typename?: 'Node', nodeLabel?: string } };

export const ChartPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"ChartParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"TimeSeriesQueryResult"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"data"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"result"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"metric"}},{"kind":"Field","name":{"kind":"Name","value":"values"}}]}}]}}]}}]} as unknown as DocumentNode<ChartPartsFragment, unknown>;
export const ChartTimeSeriesMetricFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"ChartTimeSeriesMetric"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"metric"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"Variable","name":{"kind":"Name","value":"name"}}},{"kind":"Argument","name":{"kind":"Name","value":"labels"},"value":{"kind":"ObjectValue","fields":[{"kind":"ObjectField","name":{"kind":"Name","value":"monitor"},"value":{"kind":"Variable","name":{"kind":"Name","value":"monitor"}}},{"kind":"ObjectField","name":{"kind":"Name","value":"node_id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"nodeId"}}},{"kind":"ObjectField","name":{"kind":"Name","value":"instance"},"value":{"kind":"Variable","name":{"kind":"Name","value":"instance"}}}]}},{"kind":"Argument","name":{"kind":"Name","value":"timeRange"},"value":{"kind":"Variable","name":{"kind":"Name","value":"timeRange"}}},{"kind":"Argument","name":{"kind":"Name","value":"timeRangeUnit"},"value":{"kind":"Variable","name":{"kind":"Name","value":"timeRangeUnit"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"ChartParts"}}]}}]}}]} as unknown as DocumentNode<ChartTimeSeriesMetricFragment, unknown>;
export const MetricPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"MetricParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"TimeSeriesQueryResult"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"data"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"result"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"metric"}},{"kind":"Field","name":{"kind":"Name","value":"value"}},{"kind":"Field","name":{"kind":"Name","value":"values"}}]}}]}}]}}]} as unknown as DocumentNode<MetricPartsFragment, unknown>;
export const DeviceUptimePartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"DeviceUptimeParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"deviceUptime"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"StringValue","value":"snmp_uptime_sec","block":false}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MetricParts"}}]}}]}}]} as unknown as DocumentNode<DeviceUptimePartsFragment, unknown>;
export const DeviceLatencyPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"DeviceLatencyParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"deviceLatency"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"StringValue","value":"response_time_msec","block":false}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MetricParts"}}]}}]}}]} as unknown as DocumentNode<DeviceLatencyPartsFragment, unknown>;
export const TimeSeriesMetricFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"TimeSeriesMetric"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"metric"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"Variable","name":{"kind":"Name","value":"metric"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MetricParts"}}]}}]}}]} as unknown as DocumentNode<TimeSeriesMetricFragment, unknown>;
export const MinionUptimePartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"MinionUptimeParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"minionUptime"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"StringValue","value":"minion_uptime_sec","block":false}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MetricParts"}}]}}]}}]} as unknown as DocumentNode<MinionUptimePartsFragment, unknown>;
export const MinionLatencyPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"MinionLatencyParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"minionLatency"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"StringValue","value":"response_time_msec","block":false}},{"kind":"Argument","name":{"kind":"Name","value":"labels"},"value":{"kind":"ObjectValue","fields":[{"kind":"ObjectField","name":{"kind":"Name","value":"monitor"},"value":{"kind":"Variable","name":{"kind":"Name","value":"monitor"}}},{"kind":"ObjectField","name":{"kind":"Name","value":"instance"},"value":{"kind":"Variable","name":{"kind":"Name","value":"instance"}}}]}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"status"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"MetricParts"}}]}}]}}]} as unknown as DocumentNode<MinionLatencyPartsFragment, unknown>;
export const NodeLatencyPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"NodeLatencyParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"nodeLatency"},"name":{"kind":"Name","value":"metric"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"name"},"value":{"kind":"StringValue","value":"response_time_msec","block":false}},{"kind":"Argument","name":{"kind":"Name","value":"labels"},"value":{"kind":"ObjectValue","fields":[{"kind":"ObjectField","name":{"kind":"Name","value":"node_id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}},{"kind":"ObjectField","name":{"kind":"Name","value":"monitor"},"value":{"kind":"Variable","name":{"kind":"Name","value":"monitor"}}},{"kind":"ObjectField","name":{"kind":"Name","value":"instance"},"value":{"kind":"Variable","name":{"kind":"Name","value":"instance"}}}]}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"status"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"MetricParts"}}]}}]}}]} as unknown as DocumentNode<NodeLatencyPartsFragment, unknown>;
export const NodeStatusPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"NodeStatusParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"nodeStatus"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"status"}}]}}]}}]} as unknown as DocumentNode<NodeStatusPartsFragment, unknown>;
export const NodesPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"NodesParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"findAllNodes"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"createTime"}},{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"ipInterfaces"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"ipAddress"}},{"kind":"Field","name":{"kind":"Name","value":"nodeId"}},{"kind":"Field","name":{"kind":"Name","value":"tenantId"}}]}},{"kind":"Field","name":{"kind":"Name","value":"location"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"location"}},{"kind":"Field","name":{"kind":"Name","value":"tenantId"}}]}},{"kind":"Field","name":{"kind":"Name","value":"monitoringLocationId"}},{"kind":"Field","name":{"kind":"Name","value":"nodeLabel"}},{"kind":"Field","name":{"kind":"Name","value":"tenantId"}}]}}]}}]} as unknown as DocumentNode<NodesPartsFragment, unknown>;
export const AppliancesNodesTablePartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"AppliancesNodesTableParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"findAllNodes"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"nodeLabel"}},{"kind":"Field","name":{"kind":"Name","value":"tenantId"}},{"kind":"Field","name":{"kind":"Name","value":"createTime"}},{"kind":"Field","name":{"kind":"Name","value":"monitoringLocationId"}},{"kind":"Field","name":{"kind":"Name","value":"ipInterfaces"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"ipAddress"}}]}}]}}]}}]} as unknown as DocumentNode<AppliancesNodesTablePartsFragment, unknown>;
export const MinionsTablePartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"MinionsTableParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"findAllMinions"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"label"}},{"kind":"Field","name":{"kind":"Name","value":"lastCheckedTime"}},{"kind":"Field","name":{"kind":"Name","value":"location"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"location"}}]}},{"kind":"Field","name":{"kind":"Name","value":"status"}},{"kind":"Field","name":{"kind":"Name","value":"systemId"}}]}}]}}]} as unknown as DocumentNode<MinionsTablePartsFragment, unknown>;
export const LocationsPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"LocationsParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"findAllLocations"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"location"}}]}}]}}]} as unknown as DocumentNode<LocationsPartsFragment, unknown>;
export const EventsByNodeIdPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"EventsByNodeIdParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"events"},"name":{"kind":"Name","value":"findEventsByNodeId"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"uei"}},{"kind":"Field","name":{"kind":"Name","value":"nodeId"}},{"kind":"Field","name":{"kind":"Name","value":"ipAddress"}},{"kind":"Field","name":{"kind":"Name","value":"producedTime"}}]}}]}}]} as unknown as DocumentNode<EventsByNodeIdPartsFragment, unknown>;
export const NodeByIdPartsFragmentDoc = {"kind":"Document","definitions":[{"kind":"FragmentDefinition","name":{"kind":"Name","value":"NodeByIdParts"},"typeCondition":{"kind":"NamedType","name":{"kind":"Name","value":"Query"}},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","alias":{"kind":"Name","value":"node"},"name":{"kind":"Name","value":"findNodeById"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"nodeLabel"}}]}}]}}]} as unknown as DocumentNode<NodeByIdPartsFragment, unknown>;
export const AddNodeDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"AddNode"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"node"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"NodeCreateInput"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"addNode"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"node"},"value":{"kind":"Variable","name":{"kind":"Name","value":"node"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"createTime"}},{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"monitoringLocationId"}},{"kind":"Field","name":{"kind":"Name","value":"nodeLabel"}},{"kind":"Field","name":{"kind":"Name","value":"tenantId"}}]}}]}}]} as unknown as DocumentNode<AddNodeMutation, AddNodeMutationVariables>;
export const SavePagerDutyConfigDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"mutation","name":{"kind":"Name","value":"SavePagerDutyConfig"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"config"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"PagerDutyConfigDTOInput"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"savePagerDutyConfig"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"config"},"value":{"kind":"Variable","name":{"kind":"Name","value":"config"}}}]}]}}]} as unknown as DocumentNode<SavePagerDutyConfigMutation, SavePagerDutyConfigMutationVariables>;
export const ListNodesForTableDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"ListNodesForTable"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"AppliancesNodesTableParts"}}]}},...AppliancesNodesTablePartsFragmentDoc.definitions]} as unknown as DocumentNode<ListNodesForTableQuery, ListNodesForTableQueryVariables>;
export const ListMinionsForTableDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"ListMinionsForTable"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MinionsTableParts"}}]}},...MinionsTablePartsFragmentDoc.definitions]} as unknown as DocumentNode<ListMinionsForTableQuery, ListMinionsForTableQueryVariables>;
export const ListMinionMetricsDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"ListMinionMetrics"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"instance"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"monitor"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"MinionLatencyParts"}}]}},...MinionLatencyPartsFragmentDoc.definitions,...MetricPartsFragmentDoc.definitions]} as unknown as DocumentNode<ListMinionMetricsQuery, ListMinionMetricsQueryVariables>;
export const ListNodeMetricsDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"ListNodeMetrics"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"id"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"monitor"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"instance"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"NodeLatencyParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"NodeStatusParts"}}]}},...NodeLatencyPartsFragmentDoc.definitions,...MetricPartsFragmentDoc.definitions,...NodeStatusPartsFragmentDoc.definitions]} as unknown as DocumentNode<ListNodeMetricsQuery, ListNodeMetricsQueryVariables>;
export const ListMinionsAndDevicesForTablesDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"ListMinionsAndDevicesForTables"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"AppliancesNodesTableParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"MinionsTableParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"LocationsParts"}}]}},...AppliancesNodesTablePartsFragmentDoc.definitions,...MinionsTablePartsFragmentDoc.definitions,...LocationsPartsFragmentDoc.definitions]} as unknown as DocumentNode<ListMinionsAndDevicesForTablesQuery, ListMinionsAndDevicesForTablesQueryVariables>;
export const ListLocationsForDiscoveryDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"ListLocationsForDiscovery"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"LocationsParts"}}]}},...LocationsPartsFragmentDoc.definitions]} as unknown as DocumentNode<ListLocationsForDiscoveryQuery, ListLocationsForDiscoveryQueryVariables>;
export const GetMetricDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"GetMetric"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"metric"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"TimeSeriesMetric"}}]}},...TimeSeriesMetricFragmentDoc.definitions,...MetricPartsFragmentDoc.definitions]} as unknown as DocumentNode<GetMetricQuery, GetMetricQueryVariables>;
export const GetTimeSeriesMetricDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"GetTimeSeriesMetric"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"name"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"monitor"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"nodeId"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"timeRange"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Int"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"timeRangeUnit"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"TimeRangeUnit"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"instance"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"ChartTimeSeriesMetric"}}]}},...ChartTimeSeriesMetricFragmentDoc.definitions,...ChartPartsFragmentDoc.definitions]} as unknown as DocumentNode<GetTimeSeriesMetricQuery, GetTimeSeriesMetricQueryVariables>;
export const GetNodeForGraphsDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"GetNodeForGraphs"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"id"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"findNodeById"},"arguments":[{"kind":"Argument","name":{"kind":"Name","value":"id"},"value":{"kind":"Variable","name":{"kind":"Name","value":"id"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"ipInterfaces"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"ipAddress"}}]}}]}}]}}]} as unknown as DocumentNode<GetNodeForGraphsQuery, GetNodeForGraphsQueryVariables>;
export const NodesListDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"NodesList"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"NodesParts"}}]}},...NodesPartsFragmentDoc.definitions]} as unknown as DocumentNode<NodesListQuery, NodesListQueryVariables>;
export const NodeLatencyMetricDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"NodeLatencyMetric"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"id"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"monitor"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}},{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"instance"}},"type":{"kind":"NonNullType","type":{"kind":"NamedType","name":{"kind":"Name","value":"String"}}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"NodeLatencyParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"NodeStatusParts"}}]}},...NodeLatencyPartsFragmentDoc.definitions,...MetricPartsFragmentDoc.definitions,...NodeStatusPartsFragmentDoc.definitions]} as unknown as DocumentNode<NodeLatencyMetricQuery, NodeLatencyMetricQueryVariables>;
export const NodesForMapDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"NodesForMap"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"findAllNodes"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"nodeLabel"}}]}}]}}]} as unknown as DocumentNode<NodesForMapQuery, NodesForMapQueryVariables>;
export const ListNodeStatusDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"ListNodeStatus"},"variableDefinitions":[{"kind":"VariableDefinition","variable":{"kind":"Variable","name":{"kind":"Name","value":"id"}},"type":{"kind":"NamedType","name":{"kind":"Name","value":"Long"}}}],"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"FragmentSpread","name":{"kind":"Name","value":"EventsByNodeIdParts"}},{"kind":"FragmentSpread","name":{"kind":"Name","value":"NodeByIdParts"}}]}},...EventsByNodeIdPartsFragmentDoc.definitions,...NodeByIdPartsFragmentDoc.definitions]} as unknown as DocumentNode<ListNodeStatusQuery, ListNodeStatusQueryVariables>;