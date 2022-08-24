import SeverityFilter from '@/components/Map/SeverityFilter.vue'
import { useMapStore } from '@/store/Views/mapStore'
import { findByText } from 'tests/utils'
import setupWrapper from 'tests/setupWrapper'

let wrapper: any

beforeEach(() => {
  wrapper = setupWrapper({
    component: SeverityFilter
  })
})

test('select input item should set `selectedSeverity` in store', async () => {
  const expectedValue = 'MINOR'
  const mapStore = useMapStore()
  const severitySelectInput = wrapper.get('.feather-select-input')
  
  await severitySelectInput.trigger('click')
  const minorOption = findByText(wrapper, 'span', 'Minor')
  await minorOption.trigger('click')
  
  expect(mapStore.selectedSeverity).toBe(expectedValue)
})
