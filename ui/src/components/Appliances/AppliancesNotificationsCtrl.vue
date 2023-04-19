<template>
  <FeatherButton 
    text
    data-test="notifications-btn"
    @click="openModal"
  >
    Outbound Notification
  </FeatherButton>

  <PrimaryModal title="Outbound Notifications" :visible="isVisible">
    <template v-slot:content>
      <p class="title" data-test="notifications-modal">
        PagerDuty
      </p>
      <FeatherInput
        data-test="routing-input"
        label="Integration Key"
        v-model="config.integrationkey"
      />
    </template>

    <template v-slot:footer>
      <FeatherButton 
        data-test="cancel-btn" 
        secondary 
        @click="closeModal">
          Cancel
      </FeatherButton>
      
      <FeatherButton 
        data-test="save-btn" 
        primary
        :disabled="!config.integrationkey"
        @click="save">
          Save
      </FeatherButton>
    </template>
  </PrimaryModal>
</template>

<script setup lang="ts">
import { useNotificationMutations } from '@/store/Mutations/notificationMutations'
import useModal from '@/composables/useModal'
import useSnackbar from '@/composables/useSnackbar'
import { PagerDutyConfigInput } from '@/types/graphql'

const { showSnackbar } = useSnackbar()
const { openModal, closeModal, isVisible } = useModal()
const notificationMutations = useNotificationMutations()

const config: PagerDutyConfigInput = reactive({
  integrationkey: undefined
})

const save = async () => {
  await notificationMutations.savePagerDutyIntegrationKey({ config })
  if (!notificationMutations.error) {
    config.integrationkey = undefined
    closeModal()

    showSnackbar({
      msg: 'Integration key successfuly saved.'
    })
  }
}
</script>

<style scoped lang="scss">
@use "@featherds/styles/mixins/typography";

.title {
  @include typography.headline3;
  text-align: center;
  margin-bottom: 15px;
}
</style>