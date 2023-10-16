<template>
  <PrimaryModal
    :hideTitle="true"
    :visible="isVisible"
    class="modal-delete"
  >
    <template #content>
      <p v-if="customMsg">{{ customMsg }}</p>
      <p v-else>Are you sure you wish to delete {{ name }}?</p>
    </template>
    <template #footer>
      <FeatherButton
        data-test="cancel-btn"
        secondary
        @click="closeModal"
      >
        Cancel
      </FeatherButton>
      <ButtonWithSpinner
        data-test="delete-btn"
        primary
        :click="deleteAndCloseModal"
        :isFetching="isDeleting"
      >
        {{ actionBtnText || 'Delete' }}
      </ButtonWithSpinner>
    </template>
  </PrimaryModal>
</template>

<script setup lang="ts">
const props = defineProps<{
  name?: string
  isVisible: boolean
  deleteHandler: (...p: any) => any
  closeModal: () => void
  isDeleting?: boolean
  customMsg?: string
  actionBtnText?: string
}>()

const deleteAndCloseModal = async () => {
  await props.deleteHandler()
  props.closeModal()
} 
</script>
