import Appliances from '@/containers/Appliances.vue'
import AppliancesNotificationsCtrl from '@/components/Appliances/AppliancesNotificationsCtrl.vue'
import DevicesTable from '@/components/Appliances/NodesTable.vue'
import MinionsTable from '@/components/Appliances/MinionsTable.vue'
import AppliancesAddNodeCtrl from '@/components/Appliances/AppliancesAppliancesAddNodeCtrl.vue'
import useKeycloak from '@/composables/useKeycloak'
import { KeycloakInstance } from '@dsb-norge/vue-keycloak-js/dist/types'
import setupWrapper from 'tests/setupWrapper'

const wrapper = setupWrapper({
  component: Appliances
})

it('should have a header', async () => {
  const { setKeycloak } = useKeycloak()
  await setKeycloak({ authenticated: true } as KeycloakInstance)

  const headerWelcome = wrapper.get('[data-test="header-welcome"]')
  expect(headerWelcome.exists()).toBe(true)
})

it('should have AppliancesNotificationsCtrl component', () => {
  const appliancesNotificationsCtrl = wrapper.getComponent(AppliancesNotificationsCtrl)
  expect(appliancesNotificationsCtrl.exists()).toBe(true)
})

it('should have AppliancesAddNodeCtrl component', () => {
  const addNodeCtrl = wrapper.getComponent(AppliancesAddNodeCtrl)
  expect(addNodeCtrl.exists()).toBe(true)
})

it('should have DevicesTable component', () => {
  const devicesTable = wrapper.getComponent(DevicesTable)
  expect(devicesTable.exists()).toBe(true)
})

it('should have MinionsTable component', () => {
  const minionsTable = wrapper.getComponent(MinionsTable)
  expect(minionsTable.exists()).toBe(true)
})
