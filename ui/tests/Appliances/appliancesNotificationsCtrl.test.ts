import AppliancesNotificationsCtrl from '@/components/Appliances/AppliancesNotificationsCtrl.vue'
import { useNotificationMutations } from '@/store/Mutations/notificationMutations'
import setupWrapper from 'tests/setupWrapper'

const wrapper = setupWrapper({
  component: AppliancesNotificationsCtrl,
  global: {
    stubs: {
      teleport: true
    }
  }
})

test('The component mounts', () => {
  expect(wrapper).toBeTruthy()
})

test('The modal should open on the notifications btn click', async () => {
  const btn = wrapper.get('[data-test="notifications-btn"]')
  const modal1 = wrapper.find('[data-test="notifications-modal"]')

  // modal should be closed
  expect(modal1.exists()).toBeFalsy()

  await btn.trigger('click')

  // modal should be open
  const modal2 = wrapper.find('[data-test="notifications-modal"]')
  expect(modal2.exists()).toBeTruthy()
})

test('The cancel btn should close the modal', async () => {
  await wrapper.get('[data-test="notifications-btn"]').trigger('click')
  await wrapper.get('[data-test="cancel-btn"]').trigger('click')

  const modal = wrapper.find('[data-test="notifications-modal"]')
  expect(modal.exists()).toBeFalsy()
})

test('The save btn should enable if a key is added', async () => {
  await wrapper.get('[data-test="notifications-btn"]').trigger('click')

  const input = wrapper.get('[data-test="routing-input"] .feather-input')
  const saveBtn = wrapper.get('[data-test="save-btn"]')

  // should be disabled
  expect(saveBtn.attributes('aria-disabled')).toBe('true')

  await input.setValue('key')

  // should be enabled
  expect(saveBtn.attributes('aria-disabled')).toBeUndefined()
})

test('The save mutation is called', async () => {
  const notificationMutations = useNotificationMutations()
  const sendPagerDutyRoutingKeySpy = vi.spyOn(notificationMutations, 'savePagerDutyIntegrationKey')

  await wrapper.get('[data-test="notifications-btn"]').trigger('click')

  const input = wrapper.get('[data-test="routing-input"] .feather-input')
  const saveBtn = wrapper.get('[data-test="save-btn"]')

  await input.setValue('key')
  await saveBtn.trigger('click')

  // expect send pager duty query to be called
  expect(sendPagerDutyRoutingKeySpy).toHaveBeenCalledTimes(1)
})
