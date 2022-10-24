import { MinionDto } from '@/types/graphql'

const mockData: MinionDto | undefined = {
  'id': 'minion-01',
  'status': 'UP',
  'location': 'Default',
  'lastUpdated': '2022-09-09T12:17:09.497Z'
}
export const minionFixture = (mockMinion = mockData, props: Partial<MinionDto> = {}): MinionDto => ({ ...mockMinion, ...props })