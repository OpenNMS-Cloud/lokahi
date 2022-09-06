import { mount } from '@vue/test-utils'
import DeviceTable from '@/components/Appliances/DeviceTable.vue'
import applianceQueriesStore from '../store/applianceQueries'

describe('DeviceTable.vue', () => {
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
      applianceQueriesStore(computed(() => deviceItems)) 
    })

    const requiredColumns = [
      ['Device', 'col-device'],
      ['Latency', 'col-latency'],
      ['Uptime', 'col-uptime'],
      ['Status', 'col-status']
    ]

    it.each(requiredColumns)('should have "%s" column', (_, dataTest) => {
      const wrapper = mount(DeviceTable)
       
      const elem = wrapper.find(`[data-test="${dataTest}"]`)
      expect(elem.exists()).toBe(true)
    })
  })

  describe('Device list', () => {
    it('should have an empty table when there\'s no device', () =>{
      applianceQueriesStore(computed(() => [])) 
      const wrapper = mount(DeviceTable)
         
      const deviceItem = wrapper.find('[data-test="device-item"]')
      expect(deviceItem.exists()).toBe(false)
    })
    
    it('should display a list when there\'s device', () => {
      const deviceItems = [
        {
          id: '1',
          name: 'device1',
          icmp_latency: 'UNKNOWN',
          snmp_uptime: 'UNKNOWN',
          status: 'DOWN'
        }
      ]
      applianceQueriesStore(computed(() => deviceItems)) 
      const wrapper = mount(DeviceTable)

      const deviceItem = wrapper.find('[data-test="device-item"]')
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
            id: '2',
            name: 'device2',
            icmp_latency: null,
            snmp_uptime: null,
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
        applianceQueriesStore(computed(() => deviceItems)) 
      })
  
      const formatValueBackground = (elems: any[]) => elems.map((elem: any) => {
        const css = elem.classes().filter((cl: string | string[]) => cl.indexOf('bg-') >= 0)[0]
        return [elem.attributes('data-metric'), css]
      })

      test('Latency OK/FAILED/UNKNOWN should have the corresponding background color', () => {
        const wrapper = mount(DeviceTable)
        const latencies = formatValueBackground(wrapper.findAll('[data-test="col-latency"] > .value'))
        const expectedValueBackground = [
          [undefined, 'bg-unknown'],
          [undefined, 'bg-unknown'],
          ['-10', 'bg-failed'],
          ['0', 'bg-ok'],
          ['10', 'bg-ok']
        ]

        expect(latencies).toStrictEqual(expectedValueBackground)
      })
        
      test('Uptime OK/FAILED/UNKNOWN should have the corresponding background color', () => {
        const wrapper = mount(DeviceTable)
        const uptimes = formatValueBackground(wrapper.findAll('[data-test="col-uptime"] > .value'))
        const expectedValueBackground = [
          [undefined, 'bg-unknown'],
          [undefined, 'bg-unknown'],
          ['-10', 'bg-failed'],
          ['0', 'bg-ok'],
          ['10', 'bg-ok']
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
        applianceQueriesStore(computed(() => deviceItems)) 
      })

      /**
       * Filter in status' value and background css class
       * @param elems Elements of the selector
       * @returns Array of arrays [['OK', 'bg-ok'],[...]]
       */
      const formatValueBackground = (elems: any[]) => elems.map((elem: { classes: () => any; text: () => any }) => {
        const val = ['UP','DOWN'].filter((val: string) => elem.text().indexOf(val) >= 0)[0]
        const css = elem.classes().filter((cl: string | string[]) => cl.indexOf('bg-') >= 0)[0]
        return [ val, css ]
      })

      test('Status UP/DOWN should have the corresponding background color', () => {
        const wrapper = mount(DeviceTable)

        const statuses = formatValueBackground(wrapper.findAll('[data-test="col-status"] > .value'))
        const expectedValueBackground = [
          ['UP', 'bg-ok'],
          ['DOWN', 'bg-failed']
        ]
        expect(statuses).toStrictEqual(expectedValueBackground)
      })
    })
  })
})