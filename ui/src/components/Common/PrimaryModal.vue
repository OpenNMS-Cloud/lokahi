<template>
  <FeatherDialog
    :hide-title="hideTitle"
    :modelValue="isVisible"
    :labels="labels"
    @update:modelValue="(b: any) => updateModal(b)"
    class="primary-modal"
  >
    <div
      class="content"
      :style="{ minWidth: minWidth + 'px' }"
    >
      <!-- Main content -->
      <slot name="content" />
    </div>

    <!-- Footer content -->
    <template v-slot:footer>
      <slot name="footer" />
    </template>
  </FeatherDialog>
</template>
<script setup lang="ts">
const props = defineProps({
  visible: {
    type: Boolean,
    required: true
  },
  title: {
    type: String,
    required: false
  },
  hideTitle: {
    type: Boolean,
    default: false
  },
  minWidth: {
    minWidth: Number,
    default: 450
  }
})
const emit = defineEmits(['close'])
const isVisible = computed(() => props.visible)

const labels = reactive({
  title: ' ',
  close: 'Close'
})

watchEffect(() => {
  if (props.title) {
    labels.title = props.title
  }
})
const updateModal = (isVisible: boolean) => {
  if (!isVisible) {
    emit('close')
  }
}
</script>

<style scoped lang="scss">
.content {
  min-height: 100px;
  min-width: 450px;
  position: relative;
}
</style>
<style lang="scss">
// TODO: need to find a way to scope the style below. ':deep' selector does not seem to work
@use '@featherds/styles/themes/variables';

.modal-delete {
  .dialog-body {
    display: flex;
    flex-flow: column;
    > header {
      order: 1;
    }
    .content {
      min-height: auto;
      margin-bottom: var(variables.$spacing-l);
    }
  }
}
.primary-modal a.closeButton {
  top: -18px !important;
  right: -18px !important;
  font-size: 22px !important;
}
</style>
