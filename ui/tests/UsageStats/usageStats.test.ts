import OptInOutCtrl from '@/components/UsageStats/OptInOutCtrl.vue'
import setupWrapper from 'tests/setupWrapper'
// import { useUsageStatsMutations } from '@/store/Mutations/usageStatsMutations'

const wrapper = setupWrapper({
  component: OptInOutCtrl,
  global: {
    stubs: {
      teleport: true
    }
  }
})

test('The component mounts', () => {
  expect(wrapper).toBeTruthy()
})

// test('Selecting an option makes the pinia call.', async () => {
//   const usageStatsMutations = useUsageStatsMutations()
//   const toggle = vi.spyOn(usageStatsMutations, 'toggleUsageStats')

//   const modalBtn = wrapper.get('[data-test="settings-btn"]')
//   await modalBtn.trigger('click')

//   const optInBtn = wrapper.find('[data-test="opt-in-btn"]')
//   await optInBtn.trigger('click')

//   expect(toggle).toHaveBeenCalledOnce()
// })
