import { TsResult, TimeSeriesQueryResult } from '@/types/graphql'

const mockMinionLatency: TsResult = {
  metric: {
    instance: 'opennms-minion-8d6f5f64f-4l4wh',
    job: 'horizon-core',
    location: 'Default',
    pushgateway_instance: 'horizon-core-pushgateway',
    system_id: 'opennms-minion-8d6f5f64f-4l4wh',
    node_id: 0,
    monitor: 'ECHO'
  },
  value: [1662670391.864, 2]
}

const minionLatencyFixture = (props: Partial<TsResult> = {}): TimeSeriesQueryResult => ({
  data: {
    result: [{ ...mockMinionLatency, ...props }]
  }
})

const mockMinionUptime: TsResult = {
  metric: {
    instance: 'minion-01',
    job: 'horizon-core',
    location: 'Default',
    pushgateway_instance: 'horizon-core-pushgateway'
  },
  value: [1662670391.861, 97419]
}

const minionUptimeFixture = (props: Partial<TsResult> = {}): TimeSeriesQueryResult => ({
  data: {
    result: [{ ...mockMinionUptime, ...props }]
  }
})

const mockDeviceLatency: TsResult = {
  metric: {
    instance: '127.0.0.1',
    job: 'horizon-core',
    location: 'Default',
    pushgateway_instance: 'horizon-core-pushgateway'
  },
  value: [1662657451.683, 0.17]
}

const deviceLatencyFixture = (props: Partial<TsResult> = {}): TimeSeriesQueryResult => ({
  data: {
    result: [{ ...mockDeviceLatency, ...props }]
  }
})

const mockDeviceUptime: TsResult = {}

const deviceUptimeFixture = (props: Partial<TsResult> = {}): TimeSeriesQueryResult => ({
  data: {
    result: Object.keys(mockDeviceUptime).length ? [{ ...mockDeviceUptime, ...props }] : []
  }
})

export { deviceLatencyFixture, deviceUptimeFixture, minionLatencyFixture, minionUptimeFixture }
