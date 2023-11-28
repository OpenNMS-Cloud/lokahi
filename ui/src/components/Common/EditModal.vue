<template>
  <PrimaryModal
    :hideTitle="true"
    :visible="isVisible"
  >
    <template #content>
      <div class="title">{{ title }}</div>
      <FeatherInput
        :label="inputLabel"
        :modelValue="newValue"
        @update:modelValue="updateValue"
      />
    </template>
    <template #footer>
      <FeatherButton
        secondary
        @click="closeModal"
      >
        Cancel
      </FeatherButton>
      <ButtonWithSpinner
        primary
        :click="sendMutationAndCloseModal"
        :isFetching="isCalling"
      >
        Apply
      </ButtonWithSpinner>
    </template>
  </PrimaryModal>
</template>

<script setup lang="ts">
import { fncArgVoid } from '@/types'

const newValue = ref()
const isCalling = ref(false)

const props = defineProps<{
  title: string
  inputLabel: string
  isVisible: boolean
  handler: (newVal: string) => any
  callback: () => any
  closeModal: () => void
}>()

const updateValue: fncArgVoid = (val: string) => (newValue.value = val)

const sendMutationAndCloseModal = async () => {
  isCalling.value = true
  await props.handler(newValue.value)
  isCalling.value = false
  props.callback()
  props.closeModal()
}
</script>

<style scoped lang="scss">
@use '@featherds/styles/mixins/typography';

.title {
  @include typography.headline1;
  margin-bottom: 40px;
}
</style>
