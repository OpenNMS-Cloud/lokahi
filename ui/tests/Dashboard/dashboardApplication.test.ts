import DashboardApplications from '@/components/Dashboard/DashboardApplications.vue'
import mount from 'tests/mountWithPiniaVillus'
import { useFlowsStore } from '@/store/Views/flowsStore'
import { TimeRange } from '@/types/graphql'
import { createClient, setActiveClient } from 'villus'
import { useFlowsApplicationStore } from '@/store/Views/flowsApplicationStore'

const wrapper = mount({
  component: DashboardApplications,
  shallow: false,
  stubActions: false
})

setActiveClient(
  createClient({
    url: 'http://test/graphql'
  })
)

const timeRange = { startTime: 1682025158863, endTime: 1682111558863 }
const filterValuesMock = {
  count: 10,
  step: 2000000,
  exporter: [{ nodeId: 1, ipInterfaceId: 1 }],
  timeRange,
  applications: []
}

test('The DashboardApplications component mounts correctly', () => {
  expect(wrapper).toBeTruthy()
})

test('The DashboardApplications should call querie with paramters', () => {
  const store = useFlowsStore()
  const appStore = useFlowsApplicationStore()

  store.filters.dateFilter = TimeRange.Last_24Hours
  store.filters.selectedExporterTopApplication = { value: { nodeId: 1, ipInterfaceId: 1 } }
  const spyDate = vi.spyOn(store, 'getTimeRange')
  const spy = vi.spyOn(store, 'getRequestData')
  spyDate.mockReturnValue(timeRange)
  appStore.getApplicationDataset()
  expect(spy).toHaveBeenCalled()
  expect(spy).toHaveReturnedWith(filterValuesMock)
})
