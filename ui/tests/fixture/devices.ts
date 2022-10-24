import { DeviceDto } from '@/types/graphql'

const mockData: DeviceDto | undefined = {
  'id': 1,
  'label': 'France',
  'createTime': '2022-09-07T17:52:51Z',
  'managementIp': '127.0.0.1'
}

export const deviceFixture = (mockDevice = mockData, props: Partial<DeviceDto> = {}): DeviceDto => ({ ...mockDevice, ...props })
