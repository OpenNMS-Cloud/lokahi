import { mount } from '@vue/test-utils'
import MinionsTable from '@/components/Appliances/AppliancesMinionsTable.vue'
import { setAppliancesStore } from '../store/setVillusPiniaStore'

// TODO: will fix after demo
describe.skip('MinionsTable.vue', () => {
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
      setAppliancesStore({ minions: computed(() => minionsItems) })
    })

    const requiredColumns = [
      ['Date', 'col-date'],
      ['Minion', 'col-minion'],
      ['Latency', 'col-latency'],
      ['Uptime', 'col-uptime'],
      ['Status', 'col-status'],
      ['Status', 'col-delete']
    ]

    it.each(requiredColumns)('should have "%s" column', (_, dataTest) => {
      const wrapper = mount(MinionsTable)

      const elem = wrapper.find(`[data-test="${dataTest}"]`)
      expect(elem.exists()).toBe(true)
    })
  })

  describe('Minions list', () => {
    it('should have an empty table if no minion', () => {
      setAppliancesStore({ minions: computed(() => []) })
      const wrapper = mount(MinionsTable)

      const minionItem = wrapper.find('[data-test="minion-item"]')
      expect(minionItem.exists()).toBe(false)
    })

    it('should display a list if no minion', () => {
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
      setAppliancesStore({ minions: computed(() => minionsItems) })
      const wrapper = mount(MinionsTable)

      const minionItem = wrapper.find('[data-test="minion-item"]')
      expect(minionItem.exists()).toBe(true)
    })
  })

  describe('Background color coded', () => {
    describe('Latency/Uptime', () => {
      beforeAll(() => {
        const minionsItems = [
          {
            id: '1',
            date: 'date1',
            label: 'minion1',
            icmp_latency: 10,
            snmp_uptime: 10,
            status: 'UP',
            location: 'default'
          },
          {
            id: '2',
            date: 'date2',
            label: 'minion2',
            icmp_latency: -10,
            snmp_uptime: -10,
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
        setAppliancesStore({ minions: computed(() => minionsItems) })
      })

      /**
       * Construct an array of metric value and background arrays
       * @param elems Elements of the selector
       * @returns Array of arrays [['2000', 'ok'],[...]]
       */
      const formatValueBackground = (elems: any[]) => {
        return elems.map((elem) => {
          const css = elem
            .classes()
            .filter((cl: string | string[]) => ['ok', 'failed', 'unknown'].find((bg) => bg === cl))[0]

          return [elem.attributes('data-metric'), css]
        })
      }

      test('Latency OK/FAILED/UNKNOWN should have the corresponding background color', () => {
        const wrapper = mount(MinionsTable)

        const latencies = formatValueBackground(wrapper.findAll('[data-test="minion-item-latency"]'))
        const expectedValueBackground = [
          ['10', 'ok'],
          ['-10', 'failed'],
          [undefined, 'unknown']
        ]
        expect(latencies).toStrictEqual(expectedValueBackground)
      })

      test('Uptime OK/FAILED/UNKNOWN should have the corresponding background color', () => {
        const wrapper = mount(MinionsTable)

        const uptimes = formatValueBackground(wrapper.findAll('[data-test="minion-item-uptime"]'))
        const expectedValueBackground = [
          ['10', 'ok'],
          ['-10', 'failed'],
          [undefined, 'unknown']
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
        setAppliancesStore({ minions: computed(() => minionsItems) })
      })

      /**
       * Construct an array of status and background arrays
       * @param elems Elements of the selector
       * @returns Array of arrays [['UP', 'ok'],[...]]
       */
      const formatValueBackground = (elems: any[]) => {
        return elems.map((elem) => {
          const val = ['UP', 'DOWN'].filter((val: string) => elem.text().indexOf(val) >= 0)[0]
          const css = elem
            .classes()
            .filter((cl: string | string[]) => ['ok', 'failed'].find((status) => status === cl))[0]

          return [val, css]
        })
      }

      // skipped, as no status prop available yet
      test.skip('Status UP/DOWN should have the corresponding background color', () => {
        const wrapper = mount(MinionsTable)

        const statuses = formatValueBackground(wrapper.findAll('[data-test="minion-item-status"]'))
        const expectedValueBackground = [
          ['UP', 'ok'],
          ['DOWN', 'failed']
        ]
        expect(statuses).toStrictEqual(expectedValueBackground)
      })
    })
  })

  /**
   * TODO:
   * - stub Graph component
   * - assert query arguments and result
   */
  describe.skip('Metric charts', () => {
    it('should make query `response_time_msec`', async () => {
      const minionsItems = [
        {
          id: '1',
          date: 'date1',
          label: 'minion1',
          icmp_latency: 5,
          snmp_uptime: 5,
          status: 'UP',
          location: 'default'
        }
      ]
      setAppliancesStore({ minions: computed(() => minionsItems) })
      const wrapper = mount(MinionsTable, {
        stubs: {
          PrimaryModal: true,
          Graph: true
        }
      })

      const btn = wrapper.find('[data-test="minion-item-latency"]')
      await btn.trigger('click')

      expect(false).toBe(true)
    })
  })
})
