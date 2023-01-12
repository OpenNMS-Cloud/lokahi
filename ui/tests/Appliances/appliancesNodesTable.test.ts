import { mount } from '@vue/test-utils'
import AppliancesNodesTable from '@/components/Appliances/AppliancesNodesTable.vue'
import { setAppliancesStore } from '../store/setVillusPiniaStore'

// TODO: will fix after demo
describe.skip('AppliancesNodesTable.vue', () => {
  describe('Required columns', () => {
    beforeAll(() => {
      const deviceItems = [
        {
          id: '1',
          name: 'device1',
          icmp_latency: 1,
          snmp_uptime: 0,
          status: 'UP'
        }
      ]
      setAppliancesStore({nodes: computed(() => deviceItems)})
    })

    const requiredColumns = [
      ['Device', 'col-node'],
      ['Latency', 'col-latency'],
      ['Uptime', 'col-uptime'],
      ['Status', 'col-status']
    ]

    it.each(requiredColumns)('should have "%s" column', (_, dataTest) => {
      const wrapper = mount(AppliancesNodesTable)
       
      const elem = wrapper.find(`[data-test="${dataTest}"]`)
      expect(elem.exists()).toBe(true)
    })
  })

  describe('Device list', () => {
    it('should have an empty table when there\'s no node', () =>{
      setAppliancesStore({nodes: computed(() => [])})
      const wrapper = mount(AppliancesNodesTable)
         
      const deviceItem = wrapper.find('[data-test="node-item"]')
      expect(deviceItem.exists()).toBe(false)
    })
    
    it('should display a list when there\'s node', () => {
      const deviceItems = [
        {
          id: '1',
          name: 'device1',
          icmp_latency: '604833',
          snmp_uptime: '604833',
          status: 'DOWN'
        }
      ]
      setAppliancesStore({nodes: computed(() => deviceItems)}) 
      const wrapper = mount(AppliancesNodesTable)

      const deviceItem = wrapper.find('[data-test="node-item"]')
      expect(deviceItem.exists()).toBe(true)
    })
  })

  describe('Background color coded', () => {
    describe('Latency/Uptime', () => {
      beforeAll(() => {
        const deviceItems = [
          {
            id: '1',
            name: 'device1',
            icmp_latency: undefined,
            snmp_uptime: undefined,
            status: 'DOWN'
          },
          {
            id: '3',
            name: 'device3',
            icmp_latency: -10,
            snmp_uptime: -10,
            status: 'DOWN'
          },
          {
            id: '4',
            name: 'device4',
            icmp_latency: 0,
            snmp_uptime: 0,
            status: 'UP'
          },
          {
            id: '5',
            name: 'device5',
            icmp_latency: 10,
            snmp_uptime: 10,
            status: 'UP'
          }
        ] 
        setAppliancesStore({nodes: computed(() => deviceItems)}) 
      })
  
      /**
       * Construct an array containing arrays of metric value and background
       * @param elems Elements of the selector
       * @returns Array of arrays [[undefined, 'unknown'], ['-10', 'failed'],...]
       */
      const formatValueBackground = (elems: any[]) => elems.map((elem: any) => {
        const css = elem.classes().filter((cl: string | string[]) => ['unknown', 'ok', 'failed'].find(status => status === cl))[0]

        return [elem.attributes('data-metric'), css]
      })

      test('Latency OK/FAILED/UNKNOWN should have the corresponding background color', () => {
        const wrapper = mount(AppliancesNodesTable)

        const latencies = formatValueBackground(wrapper.findAll('[data-test="col-latency"] > .value'))
        const expectedValueBackground = [
          [undefined, 'unknown'],
          ['-10', 'failed'],
          ['0', 'ok'],
          ['10', 'ok']
        ]
        expect(latencies).toStrictEqual(expectedValueBackground)
      })
        
      test('Uptime OK/FAILED/UNKNOWN should have the corresponding background color', () => {
        const wrapper = mount(AppliancesNodesTable)

        const uptimes = formatValueBackground(wrapper.findAll('[data-test="col-uptime"] > .value'))
        const expectedValueBackground = [
          [undefined, 'unknown'],
          ['-10', 'failed'],
          ['0', 'ok'],
          ['10', 'ok'] 
        ]
        expect(uptimes).toStrictEqual(expectedValueBackground)
      })
    })
    
    describe('Status', () => {
      beforeAll(() => {
        const deviceItems = [
          {
            id: '1',
            name: 'device1',
            icmp_latency: 1,
            snmp_uptime: 1000,
            status: 'UP'
          },
          {
            id: '2',
            name: 'device2',
            icmp_latency: 2000,
            snmp_uptime: 0,
            status: 'DOWN'
          }
        ] 
        setAppliancesStore({nodes: computed(() => deviceItems)}) 
      })

      /**
       * Construct an array containing arrays of status and background
       * @param elems Elements of the selector
       * @returns Array of arrays [['UP', 'ok'],[...]]
       */
      const formatValueBackground = (elems: any[]) => elems.map((elem: { classes: () => any; text: () => any }) => {
        const val = ['UP','DOWN'].filter((val: string) => elem.text().indexOf(val) >= 0)[0]
        const css = elem.classes().filter((cl: string | string[]) => ['ok', 'failed'].find(status => status === cl))[0]
        
        return [ val, css ]
      })

      test('Status UP/DOWN should have the corresponding background color', () => {
        const wrapper = mount(AppliancesNodesTable)

        const statuses = formatValueBackground(wrapper.findAll('[data-test="col-status"] > .value'))
        const expectedValueBackground = [
          ['UP', 'ok'],
          ['DOWN', 'failed']
        ]
        expect(statuses).toStrictEqual(expectedValueBackground)
      })
    })
  })
})