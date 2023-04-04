import { DefaultNode } from '@/types/graphql'

const mockData: Partial<DefaultNode> = {
  id: 1,
  nodeLabel: 'France',
  createTime: '2022-09-07T17:52:51Z',
  monitoringLocationId: 1
}

export const nodeFixture = (mockDevice = mockData, props: Partial<DefaultNode> = {}): Partial<DefaultNode> => ({ ...mockDevice, ...props })
