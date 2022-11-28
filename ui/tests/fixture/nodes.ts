import { Node } from '@/types/graphql'

const mockData: Node | undefined = {
  'id': 1,
  'nodeLabel': 'France',
  'createTime': '2022-09-07T17:52:51Z',
  'monitoringLocationId': 1
}

export const nodeFixture = (mockDevice = mockData, props: Partial<Node> = {}): Node => ({ ...mockDevice, ...props })
