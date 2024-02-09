import { TypedDocumentNode as DocumentNode } from '@graphql-typed-document-node/core'
export type Maybe<T> = T | null
export type InputMaybe<T> = Maybe<T>
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] }
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> }
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> }

/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: string;
  String: string;
  Boolean: boolean;
  Int: number;
  Float: number;
}

export type Alert = {
  __typename?: 'Alert';
  cause: Scalars['String'];
  date: Scalars['String'];
  description: Scalars['String'];
  duration: Scalars['String'];
  id: Scalars['String'];
  isAcknowledged: Scalars['Boolean'];
  isSelected?: Maybe<Scalars['Boolean']>;
  name: Scalars['String'];
  nodeType: Scalars['String'];
  severity: Scalars['String'];
  time: Scalars['String'];
}

export type AzureDiscovery = {
  __typename?: 'AzureDiscovery';
  clientId: Scalars['String'];
  directoryId: Scalars['String'];
  id: Scalars['String'];
  location: Location;
  name: Scalars['String'];
  subscriptionId: Scalars['String'];
  tags: Array<Maybe<Scalars['String']>>;
}

export type Location = {
  __typename?: 'Location';
  id: Scalars['String'];
  location: Scalars['String'];
}

export type Query = {
  __typename?: 'Query';
  alertsList: Array<Alert>;
  listAzureDiscoveries: Array<AzureDiscovery>;
}

export type AlertsListQueryVariables = Exact<{ [key: string]: never; }>

export type AlertsListQuery = { __typename?: 'Query', alertsList: Array<{ __typename?: 'Alert', id: string, name: string, severity: string, cause: string, duration: string, nodeType: string, date: string, time: string, isAcknowledged: boolean, description: string }> }

export type ListAzureDiscoveriesQueryVariables = Exact<{ [key: string]: never; }>

export type ListAzureDiscoveriesQuery = { __typename?: 'Query', listAzureDiscoveries: Array<{ __typename?: 'AzureDiscovery', id: string, name: string, clientId: string, directoryId: string, subscriptionId: string, tags: Array<string | null>, location: { __typename?: 'Location', id: string, location: string } }> }

export const AlertsListDocument = {'kind':'Document','definitions':[{'kind':'OperationDefinition','operation':'query','name':{'kind':'Name','value':'alertsList'},'selectionSet':{'kind':'SelectionSet','selections':[{'kind':'Field','name':{'kind':'Name','value':'alertsList'},'selectionSet':{'kind':'SelectionSet','selections':[{'kind':'Field','name':{'kind':'Name','value':'id'}},{'kind':'Field','name':{'kind':'Name','value':'name'}},{'kind':'Field','name':{'kind':'Name','value':'severity'}},{'kind':'Field','name':{'kind':'Name','value':'cause'}},{'kind':'Field','name':{'kind':'Name','value':'duration'}},{'kind':'Field','name':{'kind':'Name','value':'nodeType'}},{'kind':'Field','name':{'kind':'Name','value':'date'}},{'kind':'Field','name':{'kind':'Name','value':'time'}},{'kind':'Field','name':{'kind':'Name','value':'isAcknowledged'}},{'kind':'Field','name':{'kind':'Name','value':'description'}}]}}]}}]} as unknown as DocumentNode<AlertsListQuery, AlertsListQueryVariables>

export const ListAzureDiscoveriesDocument = {'kind':'Document','definitions':[{'kind':'OperationDefinition','operation':'query','name':{'kind':'Name','value':'ListAzureDiscoveries'},'selectionSet':{'kind':'SelectionSet','selections':[{'kind':'Field','name':{'kind':'Name','value':'listAzureDiscoveries'},'selectionSet':{'kind':'SelectionSet','selections':[{'kind':'Field','name':{'kind':'Name','value':'id'}},{'kind':'Field','name':{'kind':'Name','value':'name'}},{'kind':'Field','name':{'kind':'Name','value':'clientId'}},{'kind':'Field','name':{'kind':'Name','value':'directoryId'}},{'kind':'Field','name':{'kind':'Name','value':'subscriptionId'}},{'kind':'Field','name':{'kind':'Name','value':'tags'}},{'kind':'Field','name':{'kind':'Name','value':'location'},'selectionSet':{'kind':'SelectionSet','selections':[{'kind':'Field','name':{'kind':'Name','value':'id'}},{'kind':'Field','name':{'kind':'Name','value':'location'}}]}}]}}]}}]} as unknown as DocumentNode<ListAzureDiscoveriesQuery, ListAzureDiscoveriesQueryVariables>
