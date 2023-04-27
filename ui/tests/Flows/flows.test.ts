import Flows from '@/containers/Flows.vue'
import mountWithPiniaVillus from 'tests/mountWithPiniaVillus'
import { useFlowsStore } from '@/store/Views/flowsStore'
import { TimeRange } from '@/types/graphql'
import { LineGraphData, TableGraphData } from './flowsData'
import { useFlowsApplicationStore } from '@/store/Views/flowsApplicationStore'

describe('Flows', () => {
  let wrapper: any
  let appStore: any
  let store: any

  beforeAll(async () => {
    wrapper = await mountWithPiniaVillus({
      shallow: true,
      component: Flows,
      stubActions: false
    })

    vi.mock('vue-chartjs', () => ({
      Bar: () => null,
      Line: () => null
    }))

    appStore = useFlowsApplicationStore()
    store = useFlowsStore()
    store.filters.traffic.selectedItem = 'total'
    appStore.lineInboundData = LineGraphData
    appStore.lineOutboundData = LineGraphData
    appStore.lineTotalData = LineGraphData
  })

  afterAll(() => {
    wrapper.unmount()
  })

  test('The Flows page container mounts correctly', () => {
    expect(wrapper).toBeTruthy()
  })

  test('The Flows Store randomColours() should return a Hex', () => {
    const store = useFlowsStore()
    //Get random colour user store method
    const randomHex = store.randomColours(0)
    function isHex(num: string) {
      return Boolean(num.match(/^#[0-9A-F]{6}$/i))
    }
    //Check colour is a Hex
    const isValueHex = isHex(randomHex)
    expect(isValueHex).toBeTruthy()

    function isRGBA(num: string) {
      return Boolean(
        num.match(
          /^rgba[(](?:\s*0*(?:\d\d?(?:\.\d+)?(?:\s*%)?|\.\d+\s*%|100(?:\.0*)?\s*%|(?:1\d\d|2[0-4]\d|25[0-5])(?:\.\d+)?)\s*,){3}\s*0*(?:\.\d+|1(?:\.0*)?)\s*[)]$/gm
        )
      )
    }
    const randomRGBA = store.randomColours(0, true)
    const isValueRGBA = isRGBA(randomRGBA)
    expect(isValueRGBA).toBeTruthy()
  })

  test('The Flows store should populate datasets on Mount', () => {
    const store = useFlowsStore()
    expect(store.populateData).toHaveBeenCalledOnce()
    expect(store.getExporters).toHaveBeenCalledOnce()
    expect(store.getApplications).toHaveBeenCalledOnce()
    expect(store.updateApplicationCharts).toHaveBeenCalledOnce()
  })

  test('The Flows store get time range should return starttime and endtime object', () => {
    const store = useFlowsStore()
    const returnedObject = store.getTimeRange(TimeRange.Today)
    const startTime = new Date(new Date().setHours(0, 0, 0, 0)).getTime()
    const expectedObject = { startTime: startTime, endTime: Date.now() }
    expect(returnedObject).toStrictEqual(expectedObject)
  })

  test('The Flows store convert to date should convert time range to string for labels', () => {
    const store = useFlowsStore()

    const secondHourPattern = /^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$/
    const dayMonthPattern =
      /^(0[1-9]|[1-2][0-9]|3[0-1])\/(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$/

    store.filters.dateFilter = TimeRange.Today
    const returnedTodayObject = store.convertToDate('2023-01-10T01:01:25Z')
    const todayFormat = secondHourPattern.test(returnedTodayObject)
    expect(todayFormat).toBeTruthy()

    store.filters.dateFilter = TimeRange.Last_24Hours
    const returned24Object = store.convertToDate('2023-01-10T01:01:25Z')
    const dayFormat = secondHourPattern.test(returned24Object)
    expect(dayFormat).toBeTruthy()

    store.filters.dateFilter = TimeRange.SevenDays
    const returnedSevenDayObject = store.convertToDate('2023-01-10T01:01:25Z')
    const sevenFormat = dayMonthPattern.test(returnedSevenDayObject)
    expect(sevenFormat).toBeTruthy()

    store.filters.dateFilter = TimeRange.All
    const returnedDefaultObject = store.convertToDate('2023-01-10T01:01:25Z')
    const defaultFormat = dayMonthPattern.test(returnedDefaultObject)
    expect(defaultFormat).toBeTruthy()
  })

  test('The Flows store createLineChart should populate lineChartData', () => {
    appStore.lineChartData = { labels: [], datasets: [] }
    expect(appStore.lineChartData.datasets.length).toBe(0)
    vi.spyOn(appStore, 'getApplicationLineDataset').mockImplementation(async () => {
      appStore.lineInboundData = LineGraphData
      appStore.lineOutboundData = LineGraphData
      appStore.lineTotalData = LineGraphData
    })
    appStore.getApplicationLineDataset()
    appStore.createApplicationLineChartData()
    expect(appStore.lineChartData.datasets.length).toBe(2)
  })
  test('The Flows store createTableChart should populate TableGraphData', () => {
    appStore.tableChartData = { labels: [], datasets: [] }
    expect(appStore.tableChartData.datasets.length).toBe(0)
    appStore.tableData = TableGraphData
    appStore.createApplicationTableChartData()
    expect(appStore.tableChartData.datasets.length).toBe(2)
  })
})
