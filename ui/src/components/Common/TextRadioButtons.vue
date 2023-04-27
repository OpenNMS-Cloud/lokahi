<template>
  <FeatherRadioGroup
    v-model="radioModel"
    :label="''"
    class="text-radio"
    @update:modelValue="(e) => onChecked && onChecked(e)"
    :data-test="`text-radio-group-${id}`"
  >
    <FeatherRadio
      v-for="item in items"
      :id="item.value"
      :key="item.name"
      :value="item.value"
      :data-test="`text-radio-button-${item.value}`"
    >
      <p>{{ item.name }}</p>
    </FeatherRadio>
  </FeatherRadioGroup>
</template>

<script lang="ts" setup>
import { PropType } from 'vue'

const props = defineProps({
  /** Items to pass into */
  items: {
    type: Object,
    required: true,
    default: () => ({})
  },
  /** Emitted when an radio button is checked */
  onChecked: {
    type: Function as PropType<(e: any) => void>,
    default: () => ({})
  },
  /** The value of the item you want to be selected */
  selectedValue: { type: String, default: '' },
  /** ID for the Feather Group component */
  id: { type: String, default: '' }
})

const radioModel = ref(props.selectedValue)
</script>

<style scoped lang="scss">
@use '@featherds/styles/themes/variables';
@use '@/styles/vars';
@use '@featherds/styles/mixins/typography';

.text-radio :deep(.feather-radio-group) {
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
  gap: var(variables.$spacing-xs);

  .layout-container {
    margin: 0;
  }

  .layout-container .feather-radio .label {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    gap: var(variables.$spacing-xs);
    cursor: pointer;

    p {
      margin: 0;
      @include typography.button();
      color: var(variables.$disabled-text-on-surface);
    }
  }

  .layout-container .feather-radio .radio {
    display: none;
  }
  .layout-container .feather-radio[aria-checked='true'] .label {
    p {
      color: var(variables.$primary);
    }
  }
}
</style>
