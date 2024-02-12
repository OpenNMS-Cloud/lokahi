<template>
  <FeatherButton
    :primary="primary"
    :secondary="secondary"
    @click="click"
  >
    <template #icon>
      <slot />
      <FeatherSpinner
        v-if="isFetching"
        class="margin-left button-spinner"
      />
    </template>
  </FeatherButton>
</template>

<script lang="ts" setup>
import { FeatherButton } from '@featherds/button'
import { PropType } from 'vue'
import { FeatherSpinner } from '@featherds/progress'

defineProps({
  // eslint-disable-next-line @typescript-eslint/no-empty-function
  click: { type: Function as PropType<(...payload: any[]) => void>, default: () => {} },
  primary: { type: Boolean, default: false },
  secondary: { type: Boolean, default: false },
  isFetching: { type: Boolean, default: false }
})
</script>

<style lang="scss" scoped>
@use "@featherds/styles/themes/variables";
.margin-left {
  margin-left: var(variables.$spacing-m);
}
:deep(.button-spinner) .spinner circle {
  stroke: var(variables.$primary-variant);
}
</style>
