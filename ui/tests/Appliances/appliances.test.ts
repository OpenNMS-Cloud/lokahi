import Appliances from '@/containers/Appliances.vue'
import NotificationsCtrl from '@/components/Appliances/NotificationsCtrl.vue'
import DevicesTable from '@/components/Appliances/DevicesTable.vue'
import MinionsTable from '@/components/Appliances/MinionsTable.vue'
import AddDeviceCtrl from '@/components/Appliances/AddDeviceCtrl.vue'
import useKeycloak from '@/composables/useKeycloak'
import { KeycloakInstance } from '@dsb-norge/vue-keycloak-js/dist/types'
import setupWrapper from 'tests/setupWrapper'

const wrapper = setupWrapper({
  component: Appliances
})

it('should have a header',  async () => {
  const { setKeycloak } = useKeycloak()
  await setKeycloak({ authenticated: true } as KeycloakInstance)
  
  const headerWelcome = wrapper.get('[data-test="header-welcome"]')
  expect(headerWelcome.exists()).toBe(true)
})

it('should have NotificationsCtrl component', () => {
  const notificationsCtrl = wrapper.findComponent(NotificationsCtrl)
  expect(notificationsCtrl.exists()).toBe(true)
})

it('should have AddDeviceCtrl component', () => {
  const addDeviceCtrl = wrapper.findComponent(AddDeviceCtrl)
  expect(addDeviceCtrl.exists()).toBe(true)
})

it('should have DevicesTable component', () => {
  const devicesTable = wrapper.findComponent(DevicesTable)
  expect(devicesTable.exists()).toBe(true)
})

it('should have MinionsTable component', () => {
  const minionsTable = wrapper.findComponent(MinionsTable)
  expect(minionsTable.exists()).toBe(true)
})
