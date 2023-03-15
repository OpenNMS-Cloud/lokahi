import { TypedDocumentNode as DocumentNode } from '@graphql-typed-document-node/core';
export type Maybe<T> = T | null;
export type InputMaybe<T> = Maybe<T>;
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
};

export type AzureDiscovery = {
  __typename?: 'AzureDiscovery';
  clientId: Scalars['String'];
  directoryId: Scalars['String'];
  id: Scalars['String'];
  location: Location;
  name: Scalars['String'];
  subscriptionId: Scalars['String'];
  tags: Array<Maybe<Scalars['String']>>;
};

export type Location = {
  __typename?: 'Location';
  id: Scalars['String'];
  location: Scalars['String'];
};

export type Query = {
  __typename?: 'Query';
  listAzureDiscoveries: Array<AzureDiscovery>;
};

export type ListAzureDiscoveriesQueryVariables = Exact<{ [key: string]: never; }>;


export type ListAzureDiscoveriesQuery = { __typename?: 'Query', listAzureDiscoveries: Array<{ __typename?: 'AzureDiscovery', id: string, name: string, clientId: string, directoryId: string, subscriptionId: string, tags: Array<string | null>, location: { __typename?: 'Location', id: string, location: string } }> };


export const ListAzureDiscoveriesDocument = {"kind":"Document","definitions":[{"kind":"OperationDefinition","operation":"query","name":{"kind":"Name","value":"ListAzureDiscoveries"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"listAzureDiscoveries"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"name"}},{"kind":"Field","name":{"kind":"Name","value":"clientId"}},{"kind":"Field","name":{"kind":"Name","value":"directoryId"}},{"kind":"Field","name":{"kind":"Name","value":"subscriptionId"}},{"kind":"Field","name":{"kind":"Name","value":"tags"}},{"kind":"Field","name":{"kind":"Name","value":"location"},"selectionSet":{"kind":"SelectionSet","selections":[{"kind":"Field","name":{"kind":"Name","value":"id"}},{"kind":"Field","name":{"kind":"Name","value":"location"}}]}}]}}]}}]} as unknown as DocumentNode<ListAzureDiscoveriesQuery, ListAzureDiscoveriesQueryVariables>;