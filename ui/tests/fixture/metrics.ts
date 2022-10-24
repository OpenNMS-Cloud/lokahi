import { TsResult, TimeSeriesQueryResult } from '@/types/graphql'

const mockMinionLatency: TsResult = {
  'metric': {
    '__name__': 'minion_response_time_msec',
    'instance': 'minion-01',
    'job': 'horizon-core',
    'location': 'Default',
    'pushgateway_instance': 'horizon-core-pushgateway'
  },
  'value': [
    1662670391.864,
    2
  ]
}
const minionLatencyFixture = (props: Partial<TsResult> = {}): TimeSeriesQueryResult => ({
  data: { 
    result: [
      { ...mockMinionLatency, ...props }
    ]
  }
})

const mockMinionUptime: TsResult = {
  'metric': {
    '__name__': 'minion_uptime_sec',
    'instance': 'minion-01',
    'job': 'horizon-core',
    'location': 'Default',
    'pushgateway_instance': 'horizon-core-pushgateway'
  },
  'value': [
    1662670391.861,
    97419
  ]
}
const minionUptimeFixture = (props: Partial<TsResult> = {}): TimeSeriesQueryResult => ({
  data: { 
    result: [
      { ...mockMinionUptime, ...props }
    ]
  }
})

const mockDeviceLatency: TsResult = {
  'metric': {
    '__name__': 'icmp_round_trip_time_msec',
    'instance': '127.0.0.1',
    'job': 'horizon-core',
    'location': 'Default',
    'pushgateway_instance': 'horizon-core-pushgateway'
  },
  'value': [
    1662657451.683,
    0.17
  ]
}
const deviceLatencyFixture = (props: Partial<TsResult> = {}): TimeSeriesQueryResult => ({
  data: { 
    result: [
      { ...mockDeviceLatency, ...props }
    ]
  }
})

const mockDeviceUptime: TsResult = {}
const deviceUptimeFixture = (props: Partial<TsResult> = {}): TimeSeriesQueryResult => ({
  data: { 
    result: Object.keys(mockDeviceUptime).length ?
      [
        { ...mockDeviceUptime, ...props }
      ] :
      [] 
  }
})

export {
  deviceLatencyFixture,
  deviceUptimeFixture,
  minionLatencyFixture,
  minionUptimeFixture
}