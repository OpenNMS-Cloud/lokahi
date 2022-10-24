import { mount } from '@vue/test-utils'
import MinionsTable from '@/components/Appliances/MinionsTable.vue'
import { setAppliancesStore } from '../store/setVillusPiniaStore'

describe('MinionsTable.vue', () => {
  describe('Required columns', () => {
    beforeAll(() => {
      const minionsItems = [
        {
          id: '1',
          date: 'date1',
          label: 'minion1',
          icmp_latency: 5,
          snmp_uptime: 5,
          status: 'UP',
          location: 'default'
        }, 
        {
          id: '2',
          date: 'date2',
          label: 'minion2',
          icmp_latency: 5,
          snmp_uptime: 5,
          status: 'DOWN',
          location: 'default'
        }
      ] 
      setAppliancesStore({minions: computed(() => minionsItems)})
    })

    const requiredColumns = [
      ['Date', 'col-date'],
      ['Minion', 'col-minion'],
      ['Latency', 'col-latency'],
      ['Uptime', 'col-uptime'],
      ['Status', 'col-status']
    ]

    it.each(requiredColumns)('should have "%s" column', (_, dataTest) => {
      const wrapper = mount(MinionsTable)

      const elem = wrapper.find(`[data-test="${dataTest}"]`)
      expect(elem.exists()).toBe(true)
    })
  })
    
  describe('Minions list', () => {
    it('should have an empty table when there\'s no minion', () =>{
      setAppliancesStore({minions: computed(() => [])})
      const wrapper = mount(MinionsTable)
        
      const minionItem = wrapper.find('[data-test="minion-item"]')
      expect(minionItem.exists()).toBe(false)
    })
      
    it('should display a list when there\'s minion', () => {
      const minionsItems = [
        {
          id: '1',
          date: 'date1',
          label: 'minion1',
          icmp_latency: 5,
          snmp_uptime: 5,
          status: 'UP',
          location: 'default'
        }, 
        {
          id: '2',
          date: 'date2',
          label: 'minion2',
          icmp_latency: 5,
          snmp_uptime: 5,
          status: 'DOWN',
          location: 'default'
        }
      ] 
      setAppliancesStore({minions: computed(() => minionsItems)})
      const wrapper = mount(MinionsTable)
      
      const minionItem = wrapper.find('[data-test="minion-item"]')
      expect(minionItem.exists()).toBe(true)
    })
  })

  describe('Background color coded', () => {
    describe.skip('Latency/Uptime', () => {
      beforeAll(() => {
        const minionsItems = [
          {
            id: '1',
            date: 'date1',
            label: 'minion1',
            icmp_latency: 5,
            snmp_uptime: 5,
            status: 'UP',
            location: 'default'
          }, 
          {
            id: '2',
            date: 'date2',
            label: 'minion2',
            icmp_latency: 2000,
            snmp_uptime: 0,
            status: 'DOWN',
            location: 'default'
          }, 
          {
            id: '3',
            date: 'date3',
            label: 'minion3',
            icmp_latency: undefined,
            snmp_uptime: undefined,
            status: 'DOWN',
            location: 'default'
          }
        ] 
        setAppliancesStore({minions: computed(() => minionsItems)})
      })
  
      /**
       * Filter in status' value and background css class
       * @param elems Elements of the selector
       * @returns Array of arrays [['OK', 'bg-ok'],[...]]
       */
      const formatValueBackground = (elems: any[]) => {
        return elems.map((elem: { classes: () => any; text: () => any }) => {
          const val = ['OK', 'FAILED', 'UNKNOWN'].filter((val: string) => elem.text().indexOf(val) >= 0)[0]
          const css = elem.classes().filter((cl: string | string[]) => cl.indexOf('bg-') >= 0)[0]
          return [ val, css ]
        })
      }
  
      test('Latency OK/FAILED/UNKNOWN should have the corresponding background color', () => {
        const wrapper = mount(MinionsTable)
  
        const latencies = formatValueBackground(wrapper.findAll('[data-test="col-latency"]'))
        const expectedValueBackground = [
          ['OK', 'bg-ok'],
          ['FAILED', 'bg-failed'],
          ['UNKNOWN', 'bg-unknown']
        ]
        expect(latencies).toStrictEqual(expectedValueBackground)
      })
        
      test('Uptime OK/FAILED/UNKNOWN should have the corresponding background color', () => {
        const wrapper = mount(MinionsTable)
  
        const uptimes = formatValueBackground(wrapper.findAll('[data-test="col-uptime"]'))
        const expectedValueBackground = [
          ['OK', 'bg-ok'],
          ['FAILED', 'bg-failed'],
          ['UNKNOWN', 'bg-unknown']
        ]
        expect(uptimes).toStrictEqual(expectedValueBackground)
      })
    })

    describe('Status', () => {
      beforeAll(() => {
        const minionsItems = [
          {
            id: '1',
            date: 'date1',
            label: 'minion1',
            icmp_latency: 5,
            snmp_uptime: 5,
            status: 'UP',
            location: 'default'
          }, 
          {
            id: '2',
            date: 'date2',
            label: 'minion2',
            icmp_latency: 300,
            snmp_uptime: 0,
            status: 'DOWN',
            location: 'default'
          }
        ] 
        setAppliancesStore({minions: computed(() => minionsItems)}) 
      })
  
      /**
       * Filter in status' value and background css class
       * @param elems Elements of the selector
       * @returns Array of arrays [['OK', 'bg-ok'],[...]]
       */
      const formatValueBackground = (elems: any[]) => {

        return elems.map((elem: { classes: () => any; text: () => any }) => {
          const val = ['UP','DOWN'].filter((val: string) => elem.text().indexOf(val) >= 0)[0]
          const css = elem.classes().filter((cl: string | string[]) => cl.indexOf('bg-') >= 0)[0]
          return [ val, css ]
        })
      }
  
      test('Status UP/DOWN should have the corresponding background color', () => {
        const wrapper = mount(MinionsTable)

        const statuses = formatValueBackground(wrapper.findAll('[data-test="minion-item-status"]'))
        const expectedValueBackground = [
          ['UP', 'bg-ok'],
          ['DOWN', 'bg-failed']
        ]
        expect(statuses).toStrictEqual(expectedValueBackground)
      })
    })
  })
})
