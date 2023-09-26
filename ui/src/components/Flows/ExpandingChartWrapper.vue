<template>
  <FeatherExpansionPanel
    :loading="isLoading"
    :model-value="modelValue"
  >
    <template #title>
      <div class="expanding-chart-title-wrapper">
        <div class="title">{{ props.title }}</div>
        <div class="divider">|</div>
        <div class="filter-button">
          <FeatherButton
            icon="Filter"
            @click.stop.prevent="(e: any) => onFilterClick(e)"
          >
            <FeatherIcon
              class="icon"
              :icon="FilterIcon"
            />
            <FeatherIcon
              class="icon"
              :icon="DropDownIcon"
            />
          </FeatherButton>
        </div>
      </div>
    </template>
    <slot></slot>
  </FeatherExpansionPanel>
</template>

<script setup lang="ts">
import FilterIcon from '@featherds/icon/action/FilterAlt'
import DropDownIcon from '@featherds/icon/navigation/ArrowDropDown'
import { PropType } from 'vue'

const props = defineProps({
  title: {
    required: true,
    type: String
  },
  isLoading: {
    default: false,
    type: Boolean
  },
  modelValue: {
    required: true,
    type: Boolean
  },
  onFilterClick: {
    required: true,
    type: Function as PropType<(event: Event) => void>,
    default: () => ({})
  }
})
</script>

<style lang="scss" scoped>
@use '@featherds/styles/themes/variables';
@use '@/styles/vars';
@use '@/styles/mediaQueriesMixins.scss';
@import '@featherds/styles/mixins/typography';

.expanding-chart-title-wrapper {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  align-content: center;
  gap: var(--feather-spacing-m);
}
.icon {
  color: #000000;
}
.title,
.divider {
  @include body-large();
}
.filter-button .btn.btn-icon {
  border-radius: 4px;
  width: auto;
}
</style>
