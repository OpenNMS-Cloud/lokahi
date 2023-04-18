import AzureForm from '@/components/Discovery/DiscoveryAzureForm.vue'
import mount from 'tests/mountWithPiniaVillus'
import { useDiscoveryStore } from '@/store/Views/discoveryStore'
import { useDiscoveryMutations } from '@/store/Mutations/discoveryMutations'
import { AzureActiveDiscoveryCreateInput } from '@/types/graphql'
import tabIndexDirective from '@/directives/v-tabindex'

const azureTestPayload: AzureActiveDiscoveryCreateInput = {
  name: 'azure1',
  clientId: 'client1',
  clientSecret: 'secret1',
  directoryId: 'dir1',
  subscriptionId: 'sub1',
  location: 'Default',
  tags: [{ name: 'default' }]
}

const wrapper = mount({
  component: AzureForm,
  shallow: false,
  stubActions: false,
  global: {
    directives: {
      tabindex: tabIndexDirective
    }
  }
})

test('The azure form payload when saving.', async () => {
  const store = useDiscoveryStore()
  const mutations = useDiscoveryMutations()

  await wrapper.vm.selectLocation('Default')
  await wrapper.get('[data-test="azure-name-input"] .feather-input').setValue('azure1')
  await wrapper.get('[data-test="azure-client-input"] .feather-input').setValue('client1')
  await wrapper.get('[data-test="azure-secret-input"] .feather-input').setValue('secret1')
  await wrapper.get('[data-test="azure-sub-input"] .feather-input').setValue('sub1')
  await wrapper.get('[data-test="azure-dir-input"] .feather-input').setValue('dir1')
  store.saveDiscoveryAzure()

  expect(mutations.addAzureCreds).toHaveBeenCalledTimes(1)
  expect(mutations.addAzureCreds).toHaveBeenCalledWith({ discovery: azureTestPayload })
})
