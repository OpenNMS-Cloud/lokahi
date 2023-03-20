import mountWithPiniaVillus from 'tests/mountWithPiniaVillus'
import MapNodesGrid from '@/components/Map/MapNodesGrid.vue'

let wrapper: any

beforeEach(() => {
  wrapper = mountWithPiniaVillus({
    component: MapNodesGrid
  })
})

const columns = [
  ['ID', 'col-id'],
  ['FOREIGN SOURCE', 'col-foreign-source'],
  ['FOREIGN ID', 'col-foreign-id'],
  ['LABEL', 'col-label'],
  ['LABEL SOURCE', 'col-label-source'],
  // [ 'LAST CAP SCAN', 'col-last-cap-scan' ],
  // [ 'PRIMARY INTERFACE', 'col-primary-interface' ],
  ['SYSOBJECTID', 'col-sys-object-id'],
  ['SYSNAME', 'col-sys-name'],
  ['SYSDESCRIPTION', 'col-sys-description'],
  ['SYSCONTACT', 'col-sys-contact'],
  ['SYSLOCATION', 'col-sys-location']
]
it.each(columns)('should have %s column', (_, col) => {
  const elem = wrapper.get(`[data-test="${col}"]`)
  expect(elem.exists()).toBeTruthy()
})
